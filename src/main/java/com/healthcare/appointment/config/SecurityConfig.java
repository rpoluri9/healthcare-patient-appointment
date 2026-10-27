package com.healthcare.appointment.config;

import com.healthcare.appointment.repository.UserAccountRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Local authentication setup for this demo portfolio project:
 * HTTP Basic against accounts stored in the local database, with BCrypt
 * password hashes. Roles (ADMIN / STAFF / PATIENT) gate endpoints, and
 * ownership rules live in the service layer (see CurrentUserService).
 *
 * This is deliberately NOT a production identity setup: there is no external
 * OAuth/OIDC provider, no invented provider credentials, and CSRF is
 * disabled because this is a stateless JSON API exercised with Postman/curl.
 * A production deployment would use HTTPS, an OIDC provider, and CSRF
 * protection for browser clients.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public UserDetailsService userDetailsService(UserAccountRepository users) {
        return username -> users.findByUsername(username)
                .map(u -> new User(u.getUsername(), u.getPasswordHash(),
                        List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name()))))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Everyone authenticated can see who they are.
                        .requestMatchers("/api/auth/me").authenticated()
                        // Patient write endpoints: staff and admin only.
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/patients/**")
                        .hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/patients/**")
                        .hasAnyRole("STAFF", "ADMIN")
                        // Provider write endpoints: staff and admin only.
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/providers/**")
                        .hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/providers/**")
                        .hasAnyRole("STAFF", "ADMIN")
                        // Completing an appointment is a staff/admin action.
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/appointments/*/complete")
                        .hasAnyRole("STAFF", "ADMIN")
                        // Everything else appointment/patient/provider related:
                        // any authenticated role; ownership is enforced in services.
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .httpBasic(withDefaults());
        return http.build();
    }
}
