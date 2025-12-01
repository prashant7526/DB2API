package com.db2api.persistent;

import org.apache.cayenne.CayenneDataObject;
import java.util.List;

public class DbConnection extends CayenneDataObject {

    public static final String NAME_PROPERTY = "name";
    public static final String URL_PROPERTY = "url";
    public static final String USERNAME_PROPERTY = "username";
    public static final String PASSWORD_PROPERTY = "password";
    public static final String DRIVER_CLASS_PROPERTY = "driverClass";
    public static final String API_DEFINITIONS_PROPERTY = "apiDefinitions";

    public String getName() {
        return (String) readProperty(NAME_PROPERTY);
    }

    public void setName(String name) {
        writeProperty(NAME_PROPERTY, name);
    }

    public String getUrl() {
        return (String) readProperty(URL_PROPERTY);
    }

    public void setUrl(String url) {
        writeProperty(URL_PROPERTY, url);
    }

    public String getUsername() {
        return (String) readProperty(USERNAME_PROPERTY);
    }

    public void setUsername(String username) {
        writeProperty(USERNAME_PROPERTY, username);
    }

    public String getPassword() {
        return (String) readProperty(PASSWORD_PROPERTY);
    }

    public void setPassword(String password) {
        writeProperty(PASSWORD_PROPERTY, password);
    }

    public String getDriverClass() {
        return (String) readProperty(DRIVER_CLASS_PROPERTY);
    }

    public void setDriverClass(String driverClass) {
        writeProperty(DRIVER_CLASS_PROPERTY, driverClass);
    }

    @SuppressWarnings("unchecked")
    public List<ApiDefinition> getApiDefinitions() {
        return (List<ApiDefinition>) readProperty(API_DEFINITIONS_PROPERTY);
    }

    public void addToApiDefinitions(ApiDefinition obj) {
        addToManyTarget(API_DEFINITIONS_PROPERTY, obj, true);
    }

    public void removeFromApiDefinitions(ApiDefinition obj) {
        removeToManyTarget(API_DEFINITIONS_PROPERTY, obj, true);
    }
}
