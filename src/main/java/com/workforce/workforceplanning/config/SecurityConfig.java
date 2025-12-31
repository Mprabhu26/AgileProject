package com.workforce.workforceplanning.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
                .csrf(csrf -> csrf.disable())  // Disable CSRF for testing
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/", "/login", "/error").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()

                        // UI endpoints
                        .requestMatchers("/ui/projects/**").hasRole("PROJECT_MANAGER")
                        .requestMatchers("/ui/department-head/**").hasRole("DEPARTMENT_HEAD")
                        .requestMatchers("/ui/resource-planner/**").hasRole("RESOURCE_PLANNER")


                        // API endpoints
                        .requestMatchers("/projects/**").hasRole("PROJECT_MANAGER")

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
                        .password("{noop}pm123")  // {noop} means no password encoding
                        .roles("PROJECT_MANAGER")
                        .build(),
                User.withUsername("head")
                        .password("{noop}head123")
                        .roles("DEPARTMENT_HEAD")
                        .build(),
                User.withUsername("planner")
                        .password("{noop}planner123")
                        .roles("RESOURCE_PLANNER")
                        .build()
        );
    }
}