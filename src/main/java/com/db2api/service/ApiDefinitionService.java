package com.db2api.service;

import com.db2api.persistent.ApiDefinition;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.query.ObjectSelect;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApiDefinitionService {

    private final ServerRuntime serverRuntime;

    public ApiDefinitionService(ServerRuntime serverRuntime) {
        this.serverRuntime = serverRuntime;
    }

    private ObjectContext getContext() {
        return serverRuntime.newContext();
    }

    public List<ApiDefinition> getAllApiDefinitions() {
        return ObjectSelect.query(ApiDefinition.class).select(getContext());
    }

    public void saveApiDefinition(ApiDefinition apiDefinition) {
        ObjectContext context = apiDefinition.getObjectContext();
        if (context == null) {
            context = getContext();
            context.registerNewObject(apiDefinition);
        }
        context.commitChanges();
    }

    public void deleteApiDefinition(ApiDefinition apiDefinition) {
        ObjectContext context = apiDefinition.getObjectContext();
        if (context != null) {
            context.deleteObjects(apiDefinition);
            context.commitChanges();
        }
    }

    public ApiDefinition createNewApiDefinition() {
        ApiDefinition api = new ApiDefinition();
        api.setObjectContext(getContext());
        return api;
    }
}
