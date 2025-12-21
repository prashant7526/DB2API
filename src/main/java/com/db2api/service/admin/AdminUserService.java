package com.db2api.service.admin;

import com.db2api.persistent.admin.AdminUser;
import com.db2api.repository.admin.AdminUserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminUserService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(AdminUserRepository adminUserRepository, PasswordEncoder passwordEncoder) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<AdminUser> getAllUsers() {
        return adminUserRepository.findAll();
    }

    public void saveUser(AdminUser user, String rawPassword) {
        if (rawPassword != null && !rawPassword.isEmpty()) {
            user.setPassword(passwordEncoder.encode(rawPassword));
        }
        adminUserRepository.save(user);
    }

    public void deleteUser(AdminUser user) {
        adminUserRepository.delete(user);
    }

    public AdminUser createNewUser() {
        return new AdminUser();
    }
}
