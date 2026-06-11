package com.db2api.service.api;

import org.junit.jupiter.api.Test;

import java.sql.Types;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SchemaDiscoveryService#mapSqlTypeToGraphQL(int)}.
 * Verifies that SQL type constants are correctly mapped to GraphQL types.
 */
class SchemaDiscoveryServiceTest {

    @Test
    void mapSqlTypeToGraphQL_integerTypes() {
        assertEquals("Int", SchemaDiscoveryService.mapSqlTypeToGraphQL(Types.INTEGER));
        assertEquals("Int", SchemaDiscoveryService.mapSqlTypeToGraphQL(Types.SMALLINT));
        assertEquals("Int", SchemaDiscoveryService.mapSqlTypeToGraphQL(Types.TINYINT));
        assertEquals("Int", SchemaDiscoveryService.mapSqlTypeToGraphQL(Types.BIGINT));
    }

    @Test
    void mapSqlTypeToGraphQL_floatTypes() {
        assertEquals("Float", SchemaDiscoveryService.mapSqlTypeToGraphQL(Types.FLOAT));
        assertEquals("Float", SchemaDiscoveryService.mapSqlTypeToGraphQL(Types.REAL));
        assertEquals("Float", SchemaDiscoveryService.mapSqlTypeToGraphQL(Types.DOUBLE));
        assertEquals("Float", SchemaDiscoveryService.mapSqlTypeToGraphQL(Types.NUMERIC));
        assertEquals("Float", SchemaDiscoveryService.mapSqlTypeToGraphQL(Types.DECIMAL));
    }

    @Test
    void mapSqlTypeToGraphQL_booleanType() {
        assertEquals("Boolean", SchemaDiscoveryService.mapSqlTypeToGraphQL(Types.BOOLEAN));
        assertEquals("Boolean", SchemaDiscoveryService.mapSqlTypeToGraphQL(Types.BIT));
    }

    @Test
    void mapSqlTypeToGraphQL_stringTypes() {
        assertEquals("String", SchemaDiscoveryService.mapSqlTypeToGraphQL(Types.VARCHAR));
        assertEquals("String", SchemaDiscoveryService.mapSqlTypeToGraphQL(Types.CHAR));
        assertEquals("String", SchemaDiscoveryService.mapSqlTypeToGraphQL(Types.LONGVARCHAR));
        assertEquals("String", SchemaDiscoveryService.mapSqlTypeToGraphQL(Types.CLOB));
    }

    @Test
    void mapSqlTypeToGraphQL_dateTimeTypes() {
        assertEquals("String", SchemaDiscoveryService.mapSqlTypeToGraphQL(Types.DATE));
        assertEquals("String", SchemaDiscoveryService.mapSqlTypeToGraphQL(Types.TIME));
        assertEquals("String", SchemaDiscoveryService.mapSqlTypeToGraphQL(Types.TIMESTAMP));
        assertEquals("String", SchemaDiscoveryService.mapSqlTypeToGraphQL(Types.TIMESTAMP_WITH_TIMEZONE));
    }

    @Test
    void mapSqlTypeToGraphQL_unknownTypeDefaultsToString() {
        assertEquals("String", SchemaDiscoveryService.mapSqlTypeToGraphQL(Types.OTHER));
        assertEquals("String", SchemaDiscoveryService.mapSqlTypeToGraphQL(Types.BLOB));
        assertEquals("String", SchemaDiscoveryService.mapSqlTypeToGraphQL(Types.ARRAY));
    }
}
