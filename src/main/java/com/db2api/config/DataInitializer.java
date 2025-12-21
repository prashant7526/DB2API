package com.db2api.config;

import com.db2api.persistent.admin.AdminUser;
import com.db2api.service.admin.AdminUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Component responsible for initializing the application with default data upon
 * startup.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    private final AdminUserService adminUserService;

    /**
     * Constructs the DataInitializer.
     * 
     * @param adminUserService the service used to manage admin users
     */
    public DataInitializer(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /**
     * Executes initialization logic on startup.
     * 
     * @param args command line arguments
     * @throws Exception if initialization fails
     */
    @Override
    public void run(String... args) throws Exception {
        createDefaultAdminUser();
    }

    /**
     * Checks for the existence of any admin users and creates a default one if none
     * are found.
     */
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
