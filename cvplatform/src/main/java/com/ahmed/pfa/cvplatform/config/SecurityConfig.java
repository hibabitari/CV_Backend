package com.ahmed.pfa.cvplatform.config;

import com.ahmed.pfa.cvplatform.security.JwtAuthenticationEntryPoint;
import com.ahmed.pfa.cvplatform.security.JwtAuthenticationFilter;
import com.ahmed.pfa.cvplatform.security.LoginRateLimitFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security Configuration with JWT and Rate Limiting
 *
 * FEATURES:
 * - JWT-based authentication
 * - Role-based access control (ADMIN, ETUDIANT)
 * - Rate limiting on login endpoint (brute force protection)
 * - CORS configuration
 * - Stateless session management
 *
 * MODIFICATIONS:
 * - Phase 3: @EnableMethodSecurity for @PreAuthorize
 * - Phase 3: LoginRateLimitFilter added (5 attempts/min/IP)
 *
 * @author Ahmed
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private LoginRateLimitFilter loginRateLimitFilter;

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    /**
     * Password encoder bean (BCrypt)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Security filter chain configuration
     *
     * Filter Order:
     * 1. LoginRateLimitFilter (rate limiting on /api/auth/login)
     * 2. JwtAuthenticationFilter (JWT validation)
     * 3. UsernamePasswordAuthenticationFilter (Spring default)
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // Disable CSRF (Stateless API with JWT)
                .csrf(csrf -> csrf.disable())

                // Stateless session management (no HttpSession)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // 0. Preflight CORS (navigateur → React / axios / fetch)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 1. Public endpoints (Authentication & Registration)
                        .requestMatchers("/api/auth/**").permitAll()

                        // 2. Swagger UI & OpenAPI Documentation (Public)
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // 3. Actuator - Health endpoint (Public for monitoring)
                        .requestMatchers("/actuator/health").permitAll()

                        // 4. Actuator - Other endpoints (Authenticated)
                        .requestMatchers("/actuator/**").authenticated()

                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // 5. All other endpoints require valid JWT
                        .anyRequest().authenticated()
                )

                // Custom 401 Unauthorized handler
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )

                // Disable default forms (Login/Basic Auth)
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                // Add custom filters BEFORE UsernamePasswordAuthenticationFilter
                // Order matters: Rate Limit → JWT → Default
                .addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}