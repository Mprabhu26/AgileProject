package com.workforce.workforceplanning.config;

import com.workforce.workforceplanning.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF for debugging (re-enable later)
                .csrf(csrf -> csrf.disable())

                // Configure authorization ONCE
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/", "/login", "/register", "/css/**", "/js/**",
                                "/images/**", "/error", "/api/debug/**").permitAll()

                        // Role-based access
                        .requestMatchers("/ui/project-manager/**").hasRole("PROJECT_MANAGER")
                        .requestMatchers("/ui/department-head/**").hasRole("DEPARTMENT_HEAD")
                        .requestMatchers("/ui/resource-planner/**").hasRole("RESOURCE_PLANNER")
                        .requestMatchers("/ui/employee/**").hasRole("EMPLOYEE")
                        .requestMatchers("/ui/employees/**").hasAnyRole("DEPARTMENT_HEAD", "RESOURCE_PLANNER")

                        // API endpoints - allow authenticated access
                        .requestMatchers("/projects/**", "/employees/**", "/assignments/**",
                                "/applications/**", "/notifications/**").authenticated()

                        // Everything else needs authentication
                        .anyRequest().authenticated()
                )

                // Configure form login
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/perform-login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )

                // Configure logout
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll()
                )

                // Set user details service
                .userDetailsService(userDetailsService);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}