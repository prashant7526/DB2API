package com.db2api.controller;

import com.db2api.persistent.auth.RevokedToken;
import com.db2api.persistent.organization.Client;
import com.db2api.repository.auth.RevokedTokenRepository;
import com.db2api.service.EncryptionService;
import com.db2api.service.organization.OrganizationService;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for OAuth2 authentication flows.
 * Handles token issuance using the client_credentials grant type.
 */
@RestController
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final OrganizationService organizationService;
    private final EncryptionService encryptionService;
    private final RevokedTokenRepository revokedTokenRepository;

    @Value("${app.jwt.secret:verylongsecretkeythatisatleast32byteslong}")
    private String jwtSecret;

    @Value("${app.jwt.issuer:db2api}")
    private String jwtIssuer;

    @Value("${app.jwt.expiration-hours:1}")
    private int jwtExpirationHours;

    /**
     * Constructs the AuthController with required services.
     *
     * @param organizationService     the service for organization and client management
     * @param encryptionService       the service for secret decryption
     * @param revokedTokenRepository  the repository for token revocation
     */
    public AuthController(OrganizationService organizationService, EncryptionService encryptionService,
            RevokedTokenRepository revokedTokenRepository) {
        this.organizationService = organizationService;
        this.encryptionService = encryptionService;
        this.revokedTokenRepository = revokedTokenRepository;
    }

    /**
     * OAuth2 Token endpoint. Issues a JWT access token if client credentials are
     * valid.
     * 
     * @param grantType    the type of grant (must be 'client_credentials')
     * @param clientId     the identifier of the client
     * @param clientSecret the secret key of the client
     * @return a ResponseEntity containing the access token or an error message
     */
    @PostMapping("/oauth2/token")
    public ResponseEntity<?> token(@RequestParam("grant_type") String grantType,
            @RequestParam("client_id") String clientId,
            @RequestParam("client_secret") String clientSecret) {

        if (grantType == null || grantType.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "missing_grant_type"));
        }
        if (!"client_credentials".equals(grantType)) {
            return ResponseEntity.badRequest().body(Map.of("error", "unsupported_grant_type"));
        }
        if (clientId == null || clientId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "missing_client_id"));
        }
        if (clientSecret == null || clientSecret.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "missing_client_secret"));
        }

        // Find client by clientId
        Client client = organizationService.findClientByClientId(clientId);

        if (client == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "invalid_client"));
        }

        String decryptedSecret = encryptionService.decrypt(client.getClientSecret());
        if (!decryptedSecret.equals(clientSecret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "invalid_client"));
        }

        try {
            MACSigner signer = new MACSigner(jwtSecret.getBytes());
            String jti = UUID.randomUUID().toString();
            Date expiryTime = new Date(new Date().getTime() + (long) jwtExpirationHours * 3600 * 1000);
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(clientId)
                    .claim("scope", "api:read api:write") // Default scopes
                    .jwtID(jti)
                    .issuer(jwtIssuer)
                    .expirationTime(expiryTime)
                    .build();

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
            signedJWT.sign(signer);

            return ResponseEntity.ok(Map.of(
                    "access_token", signedJWT.serialize(),
                    "token_type", "Bearer",
                    "expires_in", 3600));
        } catch (Exception e) {
            logger.error("Error generating JWT token", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "server_error"));
        }
    }

    /**
     * Revokes (blacklists) a JWT token so it can no longer be used.
     * The token's JTI is stored in the database until its natural expiration.
     *
     * @param token the access token to revoke (without "Bearer " prefix)
     * @return a success or error response
     */
    @PostMapping("/oauth2/revoke")
    public ResponseEntity<?> revokeToken(@RequestParam("token") String token) {
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "missing_token"));
        }

        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            String jti = signedJWT.getJWTClaimsSet().getJWTID();
            Date expiration = signedJWT.getJWTClaimsSet().getExpirationTime();

            if (jti == null || jti.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "token_missing_jti"));
            }
            if (expiration != null && expiration.before(new Date())) {
                return ResponseEntity.ok(Map.of("status", "already_expired"));
            }

            RevokedToken revokedToken = new RevokedToken();
            revokedToken.setJti(jti);
            revokedToken.setRevokedAt(Instant.now());
            revokedToken.setExpiresAt(expiration != null ? expiration.toInstant() : Instant.now().plusSeconds(3600));
            revokedTokenRepository.save(revokedToken);

            logger.info("Token revoked: jti={}", jti);
            return ResponseEntity.ok(Map.of("status", "revoked"));
        } catch (Exception e) {
            logger.error("Error revoking token", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "server_error"));
        }
    }
}
