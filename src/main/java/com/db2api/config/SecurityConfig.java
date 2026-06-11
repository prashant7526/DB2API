package com.db2api.config;

import com.db2api.repository.auth.RevokedTokenRepository;
import com.db2api.security.CustomUserDetailsService;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.crypto.SecretKey;
import java.util.Base64;

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
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/dynamic/**", "/graphql")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
        return http.build();
    }

    /**
     * UI filter chain: form-login security for the Vaadin admin interface.
     * Replaces VaadinWebSecurity which is deprecated and incompatible with
     * the Spring Security version managed by Spring Boot.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain uiFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/vaadin/**", "/VAADIN/**"))
                .requestCache(cache -> cache.requestCache(new HttpSessionRequestCache()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                AntPathRequestMatcher.antMatcher("/login"),
                                AntPathRequestMatcher.antMatcher("/oauth2/**"),
                                AntPathRequestMatcher.antMatcher("/VAADIN/**"),
                                AntPathRequestMatcher.antMatcher("/vaadinServlet/**"),
                                AntPathRequestMatcher.antMatcher("/sw-runtime-resources-precache.js"),
                                AntPathRequestMatcher.antMatcher("/favicon.ico"),
                                AntPathRequestMatcher.antMatcher("/icons/**"),
                                AntPathRequestMatcher.antMatcher("/images/**"),
                                AntPathRequestMatcher.antMatcher("/line-awesome/**"),
                                AntPathRequestMatcher.antMatcher("/themes/**"))
                        .permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form.loginPage("/login").permitAll())
                .userDetailsService(userDetailsService);
        return http.build();
    }

    /**
     * Provides the JWT decoder bean for validating JWT tokens
     * with token revocation blocklist support.
     *
     * @return the JWT decoder
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        byte[] secretBytes = Base64.getEncoder().encode(jwtSecret.getBytes());
        SecretKey secretKey = new OctetSequenceKey.Builder(secretBytes)
                .algorithm(JWSAlgorithm.HS256)
                .build().toSecretKey();
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        return token -> {
            var jwt = decoder.decode(token);
            String jti = jwt.getClaimAsString("jti");
            if (jti != null && revokedTokenRepository.existsByJti(jti)) {
                throw new JwtValidationException("Token has been revoked",
                        java.util.List.of());
            }
            return jwt;
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
