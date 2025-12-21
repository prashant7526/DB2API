package com.db2api.config;

import com.db2api.security.CustomUserDetailsService;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Security configuration for the application using Vaadin and Spring Security.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig extends VaadinWebSecurity {

    private final CustomUserDetailsService userDetailsService;

    /**
     * Constructs the SecurityConfig with the custom user details service.
     * 
     * @param userDetailsService the service for loading user details
     */
    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    /**
     * Configures HTTP security settings, including the Vaadin login view.
     * 
     * @param http the HttpSecurity to configure
     * @throws Exception if configuration fails
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);
        setLoginView(http, com.db2api.ui.DashboardView.class);
    }

    /**
     * Provides the password encoder bean, using BCrypt.
     * 
     * @return the password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
