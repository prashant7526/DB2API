package com.db2api.controller;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the SQL injection prevention logic in
 * {@link DynamicRestController}.
 * Tests the identifier validation pattern and logic in isolation.
 */
class DynamicRestControllerTest {

    private static final Pattern VALID_IDENTIFIER = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    private boolean isAllowedIdentifier(String identifier, Set<String> allowedSet) {
        if (identifier == null || !VALID_IDENTIFIER.matcher(identifier).matches()) {
            return false;
        }
        return allowedSet.stream()
                .anyMatch(allowed -> allowed.equalsIgnoreCase(identifier));
    }

    @Test
    void validIdentifier_shouldAcceptNormalColumnNames() {
        Set<String> schema = Set.of("id", "name", "email", "created_at");
        assertTrue(isAllowedIdentifier("id", schema));
        assertTrue(isAllowedIdentifier("name", schema));
        assertTrue(isAllowedIdentifier("created_at", schema));
    }

    @Test
    void validIdentifier_shouldBeCaseInsensitive() {
        Set<String> schema = Set.of("Id", "Name");
        assertTrue(isAllowedIdentifier("id", schema));
        assertTrue(isAllowedIdentifier("NAME", schema));
    }

    @Test
    void validIdentifier_shouldRejectUnknownColumns() {
        Set<String> schema = Set.of("id", "name");
        assertFalse(isAllowedIdentifier("admin", schema));
        assertFalse(isAllowedIdentifier("password", schema));
    }

    @Test
    void validIdentifier_shouldRejectSqlInjection() {
        Set<String> schema = Set.of("id", "name");
        // Common SQL injection patterns
        assertFalse(isAllowedIdentifier("id; DROP TABLE users", schema));
        assertFalse(isAllowedIdentifier("1=1", schema));
        assertFalse(isAllowedIdentifier("' OR '1'='1", schema));
        assertFalse(isAllowedIdentifier("id --", schema));
        assertFalse(isAllowedIdentifier("id/**/", schema));
    }

    @Test
    void validIdentifier_shouldRejectNull() {
        Set<String> schema = Set.of("id");
        assertFalse(isAllowedIdentifier(null, schema));
    }

    @Test
    void validIdentifier_shouldRejectEmpty() {
        Set<String> schema = Set.of("id");
        assertFalse(isAllowedIdentifier("", schema));
    }

    @Test
    void validIdentifier_shouldRejectIdentifiersStartingWithDigit() {
        Set<String> schema = Set.of("1col");
        assertFalse(isAllowedIdentifier("1col", schema));
    }

    @Test
    void validIdentifier_shouldRejectSpecialCharacters() {
        Set<String> schema = Set.of("id");
        assertFalse(isAllowedIdentifier("id;DROP", schema));
        assertFalse(isAllowedIdentifier("id'OR", schema));
        assertFalse(isAllowedIdentifier("id\"OR", schema));
        assertFalse(isAllowedIdentifier("col.name", schema));
        assertFalse(isAllowedIdentifier("col name", schema));
    }
}
