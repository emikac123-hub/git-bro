package com.erik.git_bro.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.SecurityFilterChain;


import static org.springframework.security.config.Customizer.withDefaults;

import java.util.concurrent.Executor;

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

    private final Executor virtualThreadExecutor;

    SecurityConfig(Executor virtualThreadExecutor) {
        this.virtualThreadExecutor = virtualThreadExecutor;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(CsrfConfigurer::disable)
            .headers(headers -> headers.frameOptions(frame -> frame.disable()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(withDefaults()) // simpler for now
            .logout(logout -> logout
                .logoutSuccessUrl("/")
            );

        return http.build();
    }
}


