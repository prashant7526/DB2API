package com.db2api.config;

import com.db2api.persistent.organization.Client;
import com.db2api.persistent.organization.ClientAuthenticationMode;
import com.db2api.persistent.organization.Organization;
import com.db2api.persistent.organization.OrganizationClient;
import com.db2api.repository.auth.RevokedTokenRepository;
import com.db2api.repository.organization.ClientRepository;
import com.db2api.security.CustomUserDetailsService;
import com.db2api.service.organization.ClientBearerTokenService;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityConfigJwtDecoderTest {

    private static final String SECRET = "verylongsecretkeythatisatleast32byteslong";

    private ClientRepository clientRepository;
    private ClientBearerTokenService bearerTokenService;
    private RevokedTokenRepository revokedTokenRepository;
    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        clientRepository = mock(ClientRepository.class);
        bearerTokenService = mock(ClientBearerTokenService.class);
        revokedTokenRepository = mock(RevokedTokenRepository.class);
        securityConfig = new SecurityConfig(mock(CustomUserDetailsService.class), revokedTokenRepository);
        ReflectionTestUtils.setField(securityConfig, "jwtSecret", SECRET);
    }

    @Test
    void decodesOAuthJwtUsingTheSameSecretAsTheTokenIssuer() throws Exception {
        Client client = client("client-1", ClientAuthenticationMode.OAUTH2);
        when(clientRepository.findByClientId("client-1")).thenReturn(Optional.of(client));

        JwtDecoder decoder = securityConfig.jwtDecoder(clientRepository, bearerTokenService);
        Jwt decoded = decoder.decode(createOAuthToken("client-1"));

        assertEquals("client-1", decoded.getSubject());
    }

    @Test
    void rejectsOAuthJwtWhenTheClientUsesAnotherMode() throws Exception {
        Client client = client("client-1", ClientAuthenticationMode.BASIC);
        when(clientRepository.findByClientId("client-1")).thenReturn(Optional.of(client));

        JwtDecoder decoder = securityConfig.jwtDecoder(clientRepository, bearerTokenService);

        assertThrows(JwtException.class, () -> decoder.decode(createOAuthToken("client-1")));
    }

    @Test
    void rejectsOAuthJwtWhenTheClientIsInactive() throws Exception {
        Client client = client("client-1", ClientAuthenticationMode.OAUTH2);
        client.setActive(false);
        when(clientRepository.findByClientId("client-1")).thenReturn(Optional.of(client));

        JwtDecoder decoder = securityConfig.jwtDecoder(clientRepository, bearerTokenService);

        assertThrows(JwtException.class, () -> decoder.decode(createOAuthToken("client-1")));
    }

    @Test
    void acceptsOpaqueBearerTokenOnlyWhenItResolvesToAClient() {
        String token = ClientBearerTokenService.TOKEN_PREFIX + "opaque-token";
        Client client = client("client-1", ClientAuthenticationMode.BEARER);
        when(bearerTokenService.findClientByToken(token)).thenReturn(Optional.of(client));

        JwtDecoder decoder = securityConfig.jwtDecoder(clientRepository, bearerTokenService);

        assertEquals("client-1", decoder.decode(token).getSubject());
    }

    private Client client(String clientId, ClientAuthenticationMode mode) {
        Client client = new Client();
        client.setClientId(clientId);
        client.setAuthenticationMode(mode);
        Organization organization = new Organization();
        organization.setStatus("Active");
        OrganizationClient namedClient = new OrganizationClient();
        namedClient.setOrganization(organization);
        client.setOrganizationClient(namedClient);
        return client;
    }

    private String createOAuthToken(String clientId) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(clientId)
                .claim("scope", "api:read api:write")
                .jwtID(UUID.randomUUID().toString())
                .issuer("db2api")
                .expirationTime(Date.from(Instant.now().plusSeconds(120)))
                .build();
        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        signedJWT.sign(new MACSigner(SECRET.getBytes(StandardCharsets.UTF_8)));
        return signedJWT.serialize();
    }
}
