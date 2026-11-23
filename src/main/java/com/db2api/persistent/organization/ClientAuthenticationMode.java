package com.db2api.persistent.organization;

public enum ClientAuthenticationMode {
    BASIC("Simple login (HTTP Basic)"),
    BEARER("Bearer API token"),
    OAUTH2("OAuth2 client credentials");

    private final String label;

    ClientAuthenticationMode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
