package com.db2api.security;

import com.db2api.persistent.AdminUser;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.query.ObjectSelect;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final ServerRuntime serverRuntime;

    public CustomUserDetailsService(ServerRuntime serverRuntime) {
        this.serverRuntime = serverRuntime;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        ObjectContext context = serverRuntime.newContext();
        AdminUser adminUser = ObjectSelect.query(AdminUser.class)
                .where(AdminUser.USERNAME.eq(username))
                .selectOne(context);

        if (adminUser == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        return User.withUsername(adminUser.getUsername())
                .password(adminUser.getPassword())
                .roles(adminUser.getRole())
                .build();
    }
}
