package com.workforce.workforceplanning.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/", "/login", "/error").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()

                        // ✅ ADD THIS: Allow ALL API endpoints (for Postman testing)
                        .requestMatchers("/employees/**").permitAll()
                        .requestMatchers("/projects/**").permitAll()
                        .requestMatchers("/workflow/**").permitAll()
                        .requestMatchers("/assignments/**").permitAll()

                        // UI endpoints - Keep authentication (your teammates' work!)
                        .requestMatchers("/ui/projects/**").hasRole("PROJECT_MANAGER")
                        .requestMatchers("/ui/department-head/**").hasRole("DEPARTMENT_HEAD")
                        .requestMatchers("/ui/resource-planner/**").hasRole("RESOURCE_PLANNER")
                        .requestMatchers("/ui/employee/**").hasRole("EMPLOYEE")

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler((request, response, authentication) -> {
                            var roles = authentication.getAuthorities().stream()
                                    .map(a -> a.getAuthority())
                                    .toList();

                            if (roles.contains("ROLE_PROJECT_MANAGER")) {
                                response.sendRedirect("/ui/projects");
                            } else if (roles.contains("ROLE_DEPARTMENT_HEAD")) {
                                response.sendRedirect("/ui/department-head/dashboard");
                            } else if (roles.contains("ROLE_RESOURCE_PLANNER")) {
                                response.sendRedirect("/ui/resource-planner/dashboard");
                            } else if (roles.contains("ROLE_EMPLOYEE")) {
                                response.sendRedirect("/ui/employee/projects");
                            } else {
                                response.sendRedirect("/login?error=true");
                            }
                        })
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager(
                User.withUsername("pm")
                        .password("{noop}pm123")
                        .roles("PROJECT_MANAGER")
                        .build(),
                User.withUsername("pm2")
                        .password("{noop}pm1234")
                        .roles("PROJECT_MANAGER")
                        .build(),
                User.withUsername("pm3")
                        .password("{noop}pm1235")
                        .roles("PROJECT_MANAGER")
                        .build(),
                User.withUsername("head")
                        .password("{noop}head123")
                        .roles("DEPARTMENT_HEAD")
                        .build(),
                User.withUsername("planner")
                        .password("{noop}planner123")
                        .roles("RESOURCE_PLANNER")
                        .build(),
                User.withUsername("employee1")
                        .password("{noop}emp123")
                        .roles("EMPLOYEE")
                        .build()
        );
    }
}