package com.db2api.config;

import com.db2api.persistent.AdminUser;
import com.db2api.service.AdminUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    private final AdminUserService adminUserService;

    public DataInitializer(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @Override
    public void run(String... args) throws Exception {
        createDefaultAdminUser();
    }

    private void createDefaultAdminUser() {
        List<AdminUser> users = adminUserService.getAllUsers();
        if (users.isEmpty()) {
            logger.info("No admin users found. Creating default admin user.");
            AdminUser admin = adminUserService.createNewUser();
            admin.setUsername("admin");
            admin.setRole("ADMIN");
            adminUserService.saveUser(admin, "admin");
            logger.info("Default admin user created: username=admin, password=admin");
        } else {
            logger.info("Admin users already exist. Skipping default user creation.");
        }
    }
}
