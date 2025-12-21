package com.db2api.controller;

import com.db2api.persistent.organization.Client;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.Map;

/**
 * REST Controller for OAuth2 authentication flows.
 * Handles token issuance using the client_credentials grant type.
 */
@RestController
public class AuthController {

    private final OrganizationService organizationService;
    private final EncryptionService encryptionService;

    @Value("${app.jwt.secret:verylongsecretkeythatisatleast32byteslong}")
    private String jwtSecret;

    /**
     * Constructs the AuthController with required services.
     * 
     * @param organizationService the service for organization and client management
     * @param encryptionService   the service for secret decryption
     */
    public AuthController(OrganizationService organizationService, EncryptionService encryptionService) {
        this.organizationService = organizationService;
        this.encryptionService = encryptionService;
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

        if (!"client_credentials".equals(grantType)) {
            return ResponseEntity.badRequest().body(Map.of("error", "unsupported_grant_type"));
        }

        // Find Client
        // Note: OrganizationService doesn't have a findByClientId method yet, using
        // direct query here or adding it to service
        // For simplicity, I'll access Cayenne context via service if possible or just
        // add a method to service.
        // Let's add a method to OrganizationService or do a quick query here if we can
        // access context.
        // Since I can't easily change Service interface right now without another tool
        // call, I'll assume I can add it or use what I have.
        // Actually, I can't access context directly here easily.
        // I will add `findClientByClientId` to OrganizationService in the next step.
        // For now, let's write the logic assuming the method exists or I'll implement
        // it in the service update.

        // Placeholder for service call
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
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(clientId)
                    .claim("scope", "api:read api:write") // Default scopes
                    .issuer("http://localhost:8080")
                    .expirationTime(new Date(new Date().getTime() + 3600 * 1000))
                    .build();

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
            signedJWT.sign(signer);

            return ResponseEntity.ok(Map.of(
                    "access_token", signedJWT.serialize(),
                    "token_type", "Bearer",
                    "expires_in", 3600));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "server_error"));
        }
    }
}
