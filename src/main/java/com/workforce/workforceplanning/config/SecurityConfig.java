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
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/register", "/css/**", "/js/**", "/images/**", "/error").permitAll()
                        .requestMatchers("/ui/project-manager/**").hasRole("PROJECT_MANAGER")
                        .requestMatchers("/ui/department-head/**").hasRole("DEPARTMENT_HEAD")
                        .requestMatchers("/ui/department-head/**").hasAuthority("DEPARTMENT_HEAD")
                        .requestMatchers("/ui/resource-planner/**").hasRole("RESOURCE_PLANNER")
                        .requestMatchers("/ui/employee/**").hasRole("EMPLOYEE")
                        .requestMatchers("/ui/employees/**").hasAnyRole("DEPARTMENT_HEAD", "RESOURCE_PLANNER")

                        // ✅ ADD THIS - Allow API endpoints
                        .requestMatchers("/projects/**", "/employees/**", "/assignments/**", "/applications/**").permitAll()

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/perform-login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll()
                )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**", "/employees/**", "/projects/**", "/assignments/**")  // ✅ ADD THIS
                )
                .userDetailsService(userDetailsService);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}