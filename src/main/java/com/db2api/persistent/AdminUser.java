package com.db2api.persistent;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.exp.Property;

public class AdminUser extends CayenneDataObject {

    public static final String USERNAME_PROPERTY = "username";
    public static final String PASSWORD_PROPERTY = "password";
    public static final String ROLE_PROPERTY = "role";

    public static final Property<String> USERNAME = Property.create(USERNAME_PROPERTY, String.class);
    public static final Property<String> PASSWORD = Property.create(PASSWORD_PROPERTY, String.class);
    public static final Property<String> ROLE = Property.create(ROLE_PROPERTY, String.class);

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

    public String getRole() {
        return (String) readProperty(ROLE_PROPERTY);
    }

    public void setRole(String role) {
        writeProperty(ROLE_PROPERTY, role);
    }
}
