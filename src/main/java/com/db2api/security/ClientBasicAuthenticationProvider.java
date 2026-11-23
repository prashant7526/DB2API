package com.db2api.security;

import com.db2api.persistent.organization.Client;
import com.db2api.persistent.organization.ClientAuthenticationMode;
import com.db2api.repository.organization.ClientRepository;
import com.db2api.service.EncryptionService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
public class ClientBasicAuthenticationProvider implements AuthenticationProvider {

    private final ClientRepository clientRepository;
    private final EncryptionService encryptionService;

    public ClientBasicAuthenticationProvider(ClientRepository clientRepository, EncryptionService encryptionService) {
        this.clientRepository = clientRepository;
        this.encryptionService = encryptionService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        String clientId = authentication.getName();
        String suppliedSecret = authentication.getCredentials() == null
                ? ""
                : authentication.getCredentials().toString();

        Client client = clientRepository.findByClientId(clientId)
                .filter(candidate -> candidate.getAuthenticationMode() == ClientAuthenticationMode.BASIC
                        && candidate.isActiveInHierarchy())
                .orElseThrow(() -> new BadCredentialsException("Invalid client credentials"));

        String storedSecret = encryptionService.decrypt(client.getClientSecret());
        if (storedSecret == null || !MessageDigest.isEqual(
                storedSecret.getBytes(StandardCharsets.UTF_8),
                suppliedSecret.getBytes(StandardCharsets.UTF_8))) {
            throw new BadCredentialsException("Invalid client credentials");
        }

        return new UsernamePasswordAuthenticationToken(
                client.getClientId(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_API_CLIENT")));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
