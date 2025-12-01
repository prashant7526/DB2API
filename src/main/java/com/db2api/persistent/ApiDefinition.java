package com.db2api.persistent;

import org.apache.cayenne.CayenneDataObject;

public class ApiDefinition extends CayenneDataObject {

    public static final String TABLE_NAME_PROPERTY = "tableName";
    public static final String API_TYPE_PROPERTY = "apiType";
    public static final String ALLOWED_OPERATIONS_PROPERTY = "allowedOperations";
    public static final String INCLUDED_COLUMNS_PROPERTY = "includedColumns";
    public static final String CONNECTION_PROPERTY = "connection";

    public String getTableName() {
        return (String) readProperty(TABLE_NAME_PROPERTY);
    }

    public void setTableName(String tableName) {
        writeProperty(TABLE_NAME_PROPERTY, tableName);
    }

    public String getApiType() {
        return (String) readProperty(API_TYPE_PROPERTY);
    }

    public void setApiType(String apiType) {
        writeProperty(API_TYPE_PROPERTY, apiType);
    }

    public String getAllowedOperations() {
        return (String) readProperty(ALLOWED_OPERATIONS_PROPERTY);
    }

    public void setAllowedOperations(String allowedOperations) {
        writeProperty(ALLOWED_OPERATIONS_PROPERTY, allowedOperations);
    }

    public String getIncludedColumns() {
        return (String) readProperty(INCLUDED_COLUMNS_PROPERTY);
    }

    public void setIncludedColumns(String includedColumns) {
        writeProperty(INCLUDED_COLUMNS_PROPERTY, includedColumns);
    }

    public DbConnection getConnection() {
        return (DbConnection) readProperty(CONNECTION_PROPERTY);
    }

    public void setConnection(DbConnection connection) {
        setToOneTarget(CONNECTION_PROPERTY, connection, true);
    }
}
