package com.erik.git_bro.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the application.
 * <p>
 * This class configures HTTP security, including:
 * <ul>
 *   <li>Disabling CSRF protection (useful for non-browser clients or testing environments)</li>
 *   <li>Allowing access to the H2 database console without authentication</li>
 *   <li>Disabling frame options headers to allow the H2 console UI to be embedded</li>
 *   <li>Requiring authentication for all other requests</li>
 *   <li>Enabling HTTP Basic authentication for simplicity</li>
 * </ul>
 * </p>
 * <p>
 * Note: Disabling CSRF and frame options headers should be carefully considered in production
 * environments to avoid security risks.
 * </p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    /**
     * Defines the security filter chain for HTTP requests.
     *
     * @param http the {@link HttpSecurity} to configure
     * @return the built {@link SecurityFilterChain}
     * @throws Exception if an error occurs while configuring security
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable() // Disable CSRF protection for simplicity or testing
            .headers(headers -> headers
                .frameOptions(frame -> frame.disable()) // Allow use of frames for H2 console
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**").permitAll() // Allow unrestricted access to H2 console
                .anyRequest().authenticated() // Require authentication for all other requests
            )
            .httpBasic(); // Enable HTTP Basic authentication

        return http.build();
    }
}
