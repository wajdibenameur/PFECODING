package tn.iteam.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for the existing-service acting as an
 * OAuth2 Resource Server that validates Keycloak-issued JWTs.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final KeycloakJwtConverter keycloakJwtConverter;

    public SecurityConfig(KeycloakJwtConverter keycloakJwtConverter) {
        this.keycloakJwtConverter = keycloakJwtConverter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            /*
             * CSRF protection is intentionally disabled for this REST API.
             * All endpoints are protected via JWT Bearer tokens (Authorization header),
             * not cookie-based sessions. CSRF attacks require a browser to automatically
             * attach credentials (cookies), which is not the case here.
             * See: https://docs.spring.io/spring-security/reference/features/exploits/csrf.html#csrf-when
             */
            .csrf(AbstractHttpConfigurer::disable)
            // Stateless sessions
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    // Actuator health is public
                    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                    // Allow pre-flight OPTIONS requests
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    // WebSocket handshake endpoints (SockJS negotiation uses HTTP)
                    .requestMatchers("/ws/**").permitAll()
                    // Everything else requires a valid JWT
                    .anyRequest().authenticated()
            )
            // Configure as OAuth2 Resource Server with JWT support
            .oauth2ResourceServer(oauth2 ->
                    oauth2.jwt(jwt ->
                            jwt.jwtAuthenticationConverter(keycloakJwtConverter)
                    )
            );

        return http.build();
    }
}
