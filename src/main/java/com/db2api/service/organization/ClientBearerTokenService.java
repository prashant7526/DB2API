package com.db2api.service.organization;

import com.db2api.persistent.organization.Client;
import com.db2api.persistent.organization.ClientAuthenticationMode;
import com.db2api.repository.organization.ClientRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class ClientBearerTokenService {

    public static final String TOKEN_PREFIX = "db2_";

    private final ClientRepository clientRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public ClientBearerTokenService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public String generateToken(Client client) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String token = TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        client.setBearerTokenHash(hashToken(token));
        return token;
    }

    public Optional<Client> findClientByToken(String token) {
        if (token == null || !token.startsWith(TOKEN_PREFIX)) {
            return Optional.empty();
        }
        return clientRepository.findByBearerTokenHash(hashToken(token))
                .filter(client -> client.getAuthenticationMode() == ClientAuthenticationMode.BEARER
                        && client.isActiveInHierarchy());
    }

    static String hashToken(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
