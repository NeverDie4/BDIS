package com.bdis.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

@Configuration
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
        "/map-points",
        "/map-points/**",
        "/files",
        "/files/**",
        "/v3/api-docs/**",
        "/swagger-ui.html",
        "/swagger-ui/**"
    };

    private static final String[] M16_PATHS = {
        "/evaluation-standards",
        "/evaluation-standards/**",
        "/evaluation-tasks",
        "/evaluation-tasks/**",
        "/evaluation-records",
        "/evaluation-records/**"
    };

    private static final String[] M17_PATHS = {
        "/declarations",
        "/declarations/**",
        "/declaration-archives",
        "/declaration-archives/**"
    };

    private static final String[] M18_PATHS = {
        "/performances",
        "/performances/**",
        "/performance-standards",
        "/performance-standards/**",
        "/performance-statistics",
        "/performance-statistics/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, @Value("${server.servlet.context-path:}") String contextPath)
            throws Exception {
        return http.cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(HttpMethod.OPTIONS, "/**")
                                        .permitAll()
                                        .requestMatchers(publicPaths(contextPath))
                                        .permitAll()
                                        .requestMatchers(pathsWithContext(contextPath, M16_PATHS))
                                        .hasAnyRole("ADMIN", "M16")
                                        .requestMatchers(pathsWithContext(contextPath, M17_PATHS))
                                        .hasAnyRole("ADMIN", "M17")
                                        .requestMatchers(pathsWithContext(contextPath, M18_PATHS))
                                        .hasAnyRole("ADMIN", "M18")
                                        .anyRequest()
                                        .authenticated())
                .build();
    }

    @Bean
    public UserDetailsService userDetailsService(
            PasswordEncoder passwordEncoder,
            @Value("${bdis.security.dev-user:admin}") String username,
            @Value("${bdis.security.dev-password:admin123}") String password,
            @Value("${bdis.security.dev-roles:ADMIN,M16,M17,M18}") String roles) {
        String[] roleNames =
                Arrays.stream(roles.split(","))
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .toArray(String[]::new);
        return new InMemoryUserDetailsManager(
                User.withUsername(username)
                        .password(passwordEncoder.encode(password))
                        .roles(roleNames)
                        .build());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static String[] publicPaths(String contextPath) {
        return pathsWithContext(contextPath, PUBLIC_PATHS);
    }

    private static String[] pathsWithContext(String contextPath, String[] sourcePaths) {
        List<String> paths = new ArrayList<>();
        for (String path : sourcePaths) {
            paths.add(path);
        }
        String normalizedContextPath = normalizeContextPath(contextPath);
        if (StringUtils.hasText(normalizedContextPath)) {
            for (String path : sourcePaths) {
                paths.add(normalizedContextPath + path);
            }
        }
        return paths.toArray(String[]::new);
    }

    private static String normalizeContextPath(String contextPath) {
        if (!StringUtils.hasText(contextPath) || "/".equals(contextPath)) {
            return "";
        }
        String normalized = contextPath.startsWith("/") ? contextPath : "/" + contextPath;
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }
}
