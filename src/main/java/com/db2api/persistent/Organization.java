package com.db2api.persistent;

import org.apache.cayenne.CayenneDataObject;
import java.util.List;

public class Organization extends CayenneDataObject {

    public static final String NAME_PROPERTY = "name";
    public static final String STATUS_PROPERTY = "status";
    public static final String CLIENTS_PROPERTY = "clients";

    public String getName() {
        return (String) readProperty(NAME_PROPERTY);
    }

    public void setName(String name) {
        writeProperty(NAME_PROPERTY, name);
    }

    public String getStatus() {
        return (String) readProperty(STATUS_PROPERTY);
    }

    public void setStatus(String status) {
        writeProperty(STATUS_PROPERTY, status);
    }

    @SuppressWarnings("unchecked")
    public List<Client> getClients() {
        return (List<Client>) readProperty(CLIENTS_PROPERTY);
    }

    public void addToClients(Client obj) {
        addToManyTarget(CLIENTS_PROPERTY, obj, true);
    }

    public void removeFromClients(Client obj) {
        removeToManyTarget(CLIENTS_PROPERTY, obj, true);
    }
}
