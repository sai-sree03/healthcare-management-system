package com.healthcare.hms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import java.nio.charset.StandardCharsets;

/**
 * Role matrix (enforced on the server - hiding buttons in the UI is only a convenience):
 *
 * ADMIN         everything, plus user management; can delete (deactivate) patients, doctors, records
 * RECEPTIONIST  patients (create/edit), appointments (book/edit/cancel); no medical records, no admin
 * DOCTOR        read patients/doctors/own appointments; complete or cancel own appointments;
 *               create and edit their own medical records
 *
 * Finer rules that depend on WHO owns the data (e.g. "a doctor can only edit their own records")
 * live in the services.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String JSON = "application/json";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)

            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; "
                    + "frame-ancestors 'none'; base-uri 'self'; form-action 'self'")))

            .authorizeHttpRequests(auth -> auth
                // public: landing page, login page and static assets
                .requestMatchers("/", "/index.html", "/login.html", "/favicon.ico",
                                 "/css/**", "/js/**", "/error",
                                 "/actuator/health", "/actuator/health/**").permitAll()

                // administration
                .requestMatchers("/api/users/**", "/users.html").hasRole("ADMIN")

                // patients
                .requestMatchers(HttpMethod.GET, "/api/patients/**").hasAnyRole("ADMIN", "RECEPTIONIST", "DOCTOR")
                .requestMatchers(HttpMethod.POST, "/api/patients/**").hasAnyRole("ADMIN", "RECEPTIONIST")
                .requestMatchers(HttpMethod.PUT, "/api/patients/**").hasAnyRole("ADMIN", "RECEPTIONIST")
                .requestMatchers(HttpMethod.DELETE, "/api/patients/**").hasRole("ADMIN")

                // doctors
                .requestMatchers(HttpMethod.GET, "/api/doctors/**").hasAnyRole("ADMIN", "RECEPTIONIST", "DOCTOR")
                .requestMatchers("/api/doctors/**").hasRole("ADMIN")

                // appointments (status changes are checked per role/owner in the service)
                .requestMatchers(HttpMethod.GET, "/api/appointments/**").hasAnyRole("ADMIN", "RECEPTIONIST", "DOCTOR")
                .requestMatchers(HttpMethod.PATCH, "/api/appointments/**").hasAnyRole("ADMIN", "RECEPTIONIST", "DOCTOR")
                .requestMatchers(HttpMethod.POST, "/api/appointments/**").hasAnyRole("ADMIN", "RECEPTIONIST")
                .requestMatchers(HttpMethod.PUT, "/api/appointments/**").hasAnyRole("ADMIN", "RECEPTIONIST")

                // medical records: clinical data, so receptionists never see it
                .requestMatchers(HttpMethod.GET, "/api/medical-records/**", "/medical-records.html").hasAnyRole("ADMIN", "DOCTOR")
                .requestMatchers(HttpMethod.POST, "/api/medical-records/**").hasRole("DOCTOR")
                .requestMatchers(HttpMethod.PUT, "/api/medical-records/**").hasRole("DOCTOR")
                .requestMatchers(HttpMethod.DELETE, "/api/medical-records/**").hasRole("ADMIN")

                // everything else (dashboard, other pages, /api/auth/me, /api/dashboard/**) needs a login
                .anyRequest().authenticated())

            .formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl("/api/auth/login")
                .successHandler((request, response, authentication) -> {
                    response.setStatus(200);
                    response.setContentType(JSON);
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.getWriter().write("{\"status\":\"ok\"}");
                })
                .failureHandler((request, response, exception) ->
                    writeJson(response, 401, "Invalid username or password."))
                .permitAll())

            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((request, response, authentication) -> response.setStatus(200))
                .deleteCookies("JSESSIONID"))

            .exceptionHandling(ex -> ex
                // not logged in: API calls get 401 JSON, browser pages get sent to the login page
                .authenticationEntryPoint((request, response, authException) -> {
                    if (request.getRequestURI().startsWith("/api/")) {
                        writeJson(response, 401, "Please log in.");
                    } else {
                        response.sendRedirect("/login.html");
                    }
                })
                // logged in but wrong role: API calls get 403 JSON, pages bounce back to the dashboard
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    if (request.getRequestURI().startsWith("/api/")) {
                        writeJson(response, 403, "You do not have permission to do this.");
                    } else {
                        response.sendRedirect("/dashboard.html?denied=1");
                    }
                }));

        return http.build();
    }

    private static void writeJson(jakarta.servlet.http.HttpServletResponse response, int status, String message)
            throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(JSON);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // messages here are fixed literals, so no JSON escaping is needed
        response.getWriter().write("{\"status\":" + status + ",\"message\":\"" + message + "\"}");
    }
}
