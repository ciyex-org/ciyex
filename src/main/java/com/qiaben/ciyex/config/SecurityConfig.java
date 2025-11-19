package com.qiaben.ciyex.config;

import com.qiaben.ciyex.security.KeycloakJwtAuthenticationConverter;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @Value("${jwt.secret:portal-secret-key-for-development-only}")
    private String jwtSecret;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:https://aran-stg.zpoa.com/realms/master}")
    private String issuerUri;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ✅ Enable CORS for frontend integration
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // ✅ Disable CSRF for REST APIs
                .csrf(csrf -> csrf.disable())
                // ✅ Stateless session (each request must have JWT)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // ✅ Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints (no authentication required)
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/public/**",
                                "/api/portal/auth/**",
                                "/api/portal/approvals/**",
                                "/api/admin/templates/**",  // ✅ Allow public access to admin templates for testing
                                "/actuator/**"
                        ).permitAll()

                        // Role-based access control
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/provider/**").hasAnyRole("PROVIDER", "ADMIN")
                        .requestMatchers("/api/portal/**").hasAnyRole("PATIENT", "PROVIDER", "ADMIN")
                        .requestMatchers("/api/fhir/**").hasAnyRole("PATIENT", "PROVIDER", "ADMIN") // ✅ Added for FHIR endpoints

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                // ✅ Use Keycloak JWT as authentication source
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt
                                .decoder(combinedJwtDecoder())
                                .jwtAuthenticationConverter(keycloakJwtAuthenticationConverter)
                        )
                );

        return http.build();
    }

    /**
     * ✅ Hybrid JWT Decoder:
     *  1. Tries Keycloak RS256 public key validation.
     *  2. Falls back to local HS256 secret for development.
     */
    @Bean
    public JwtDecoder combinedJwtDecoder() {
        try {
            System.out.println("✅ Initializing Keycloak JWT Decoder from issuer: " + issuerUri);
            return JwtDecoders.fromIssuerLocation(issuerUri);
        } catch (Exception e) {
            System.out.println("⚠️ Failed to load Keycloak JWKS, using fallback local JWT decoder: " + e.getMessage());

            // ✅ Decode the Base64-encoded secret from application.yml
            byte[] keyBytes = java.util.Base64.getDecoder().decode(jwtSecret);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");

            return NimbusJwtDecoder.withSecretKey(secretKey).build();
        }
    }

    /**
     * ✅ BCrypt encoder for user password hashing (if local login ever used)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * ✅ CORS configuration for frontend communication (local + staging + prod)
     */
    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("http://localhost:3000");
        config.addAllowedOriginPattern("https://aran-stg.zpoa.com");
        config.addAllowedOriginPattern("https://portal.ciyex.com");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
