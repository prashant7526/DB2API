package com.db2api.persistent;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.exp.Property;

public class Client extends CayenneDataObject {

    public static final String CLIENT_ID_PROPERTY = "clientId";
    public static final String CLIENT_SECRET_PROPERTY = "clientSecret";
    public static final String ORGANIZATION_PROPERTY = "organization";
    
    // Cayenne property names for queries
    public static final Property<String> CLIENT_ID = Property.create(CLIENT_ID_PROPERTY, String.class);
    public static final Property<String> CLIENT_SECRET = Property.create(CLIENT_SECRET_PROPERTY, String.class);
    public static final Property<Organization> ORGANIZATION = Property.create(ORGANIZATION_PROPERTY, Organization.class);

    public String getClientId() {
        return (String) readProperty(CLIENT_ID_PROPERTY);
    }

    public void setClientId(String clientId) {
        writeProperty(CLIENT_ID_PROPERTY, clientId);
    }

    public String getClientSecret() {
        return (String) readProperty(CLIENT_SECRET_PROPERTY);
    }

    public void setClientSecret(String clientSecret) {
        writeProperty(CLIENT_SECRET_PROPERTY, clientSecret);
    }

    public Organization getOrganization() {
        return (Organization) readProperty(ORGANIZATION_PROPERTY);
    }

    public void setOrganization(Organization organization) {
        setToOneTarget(ORGANIZATION_PROPERTY, organization, true);
    }
}
