package com.db2api.config;

import com.db2api.persistent.organization.Client;
import com.db2api.persistent.organization.ClientAuthenticationMode;
import com.db2api.repository.auth.RevokedTokenRepository;
import com.db2api.repository.organization.ClientRepository;
import com.db2api.security.ClientBasicAuthenticationProvider;
import com.db2api.security.CustomUserDetailsService;
import com.db2api.service.organization.ClientBearerTokenService;
import com.db2api.ui.LoginView;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Security configuration with two filter chains:
 * - API chain: JWT-based stateless auth for /api/dynamic/** and /graphql
 * - UI chain: form-based login for the Vaadin admin interface
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final RevokedTokenRepository revokedTokenRepository;

    @Value("${app.jwt.secret:verylongsecretkeythatisatleast32byteslong}")
    private String jwtSecret;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
            RevokedTokenRepository revokedTokenRepository) {
        this.userDetailsService = userDetailsService;
        this.revokedTokenRepository = revokedTokenRepository;
    }

    /**
     * API filter chain (highest priority): JWT-based stateless security
     * for the dynamic REST and GraphQL endpoints.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(
            HttpSecurity http,
            ClientBasicAuthenticationProvider clientBasicAuthenticationProvider,
            JwtDecoder jwtDecoder) throws Exception {
        http.securityMatcher("/api/dynamic/**", "/graphql")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(clientBasicAuthenticationProvider)
                .httpBasic(Customizer.withDefaults())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder)))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
        return http.build();
    }

    /**
     * UI filter chain: Vaadin-aware form login and view authorization.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain uiFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/vaadin/**", "/VAADIN/**"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/oauth2/**",
                                "/VAADIN/**",
                                "/vaadinServlet/**",
                                "/sw-runtime-resources-precache.js",
                                "/favicon.ico",
                                "/icons/**",
                                "/images/**",
                                "/line-awesome/**",
                                "/themes/**")
                        .permitAll())
                .userDetailsService(userDetailsService)
                .with(VaadinSecurityConfigurer.vaadin(), vaadin -> vaadin.loginView(LoginView.class));
        return http.build();
    }

    /**
     * Provides the JWT decoder bean for validating JWT tokens
     * with token revocation blocklist support.
     *
     * @return the JWT decoder
     */
    @Bean
    public JwtDecoder jwtDecoder(
            ClientRepository clientRepository,
            ClientBearerTokenService bearerTokenService) {
        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        SecretKey secretKey = new OctetSequenceKey.Builder(secretBytes)
                .algorithm(JWSAlgorithm.HS256)
                .build().toSecretKey();
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        return token -> {
            if (token.startsWith(ClientBearerTokenService.TOKEN_PREFIX)) {
                Client client = bearerTokenService.findClientByToken(token)
                        .orElseThrow(() -> new JwtException("Invalid bearer token"));
                return Jwt.withTokenValue(token)
                        .header("alg", "opaque")
                        .subject(client.getClientId())
                        .claim("authentication_mode", ClientAuthenticationMode.BEARER.name())
                        .build();
            }

            var jwt = decoder.decode(token);
            String jti = jwt.getClaimAsString("jti");
            if (jti != null && revokedTokenRepository.existsByJti(jti)) {
                throw new JwtValidationException("Token has been revoked",
                        java.util.List.of());
            }
            Client client = clientRepository.findByClientId(jwt.getSubject())
                    .filter(candidate -> candidate.getAuthenticationMode() == ClientAuthenticationMode.OAUTH2
                            && candidate.isActiveInHierarchy())
                    .orElseThrow(() -> new JwtException("OAuth2 is not enabled for this client"));
            return jwt;
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
