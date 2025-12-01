package com.db2api.service;

import com.db2api.persistent.AdminUser;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.query.ObjectSelect;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminUserService {

    private final ServerRuntime serverRuntime;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(ServerRuntime serverRuntime, PasswordEncoder passwordEncoder) {
        this.serverRuntime = serverRuntime;
        this.passwordEncoder = passwordEncoder;
    }

    private ObjectContext getContext() {
        return serverRuntime.newContext();
    }

    public List<AdminUser> getAllUsers() {
        return ObjectSelect.query(AdminUser.class).select(getContext());
    }

    public void saveUser(AdminUser user, String rawPassword) {
        if (rawPassword != null && !rawPassword.isEmpty()) {
            user.setPassword(passwordEncoder.encode(rawPassword));
        }
        user.getObjectContext().commitChanges();
    }

    public void deleteUser(AdminUser user) {
        user.getObjectContext().deleteObjects(user);
        user.getObjectContext().commitChanges();
    }

    public AdminUser createNewUser() {
        ObjectContext context = getContext();
        AdminUser user = context.newObject(AdminUser.class);
        return user;
    }
}
