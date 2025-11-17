package com.qiaben.ciyex.config;

import com.qiaben.ciyex.security.JwtAuthenticationFilter;
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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired(required = false)
    @Nullable
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @Value("${jwt.secret:portal-secret-key-for-development-only}")
    private String jwtSecret;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:https://aran-stg.zpoa.com/realms/master}")
    private String issuerUri;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ✅ Enable CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints (unsecured)
                .requestMatchers(
                    "/api/auth/**",
                    "/api/public/**",
                    "/api/portal/auth/**",
                    "/api/portal/approvals/**",
                    "/actuator/**"
                ).permitAll()

                // Role-based access
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/provider/**").hasAnyRole("PROVIDER", "ADMIN")
                .requestMatchers("/api/portal/**").hasAnyRole("PATIENT", "PROVIDER", "ADMIN")

                // Secure everything else
                .anyRequest().authenticated()
            )

            // ✅ Enable OAuth2 resource server for Keycloak JWT
            .oauth2ResourceServer(oauth -> oauth
                .jwt(jwt -> jwt
                    .decoder(combinedJwtDecoder()) // Hybrid decoder
                    .jwtAuthenticationConverter(keycloakJwtAuthenticationConverter)
                )
            );

        // ✅ Add local JWT authentication filter (for /api/auth/login)
        if (jwtAuthenticationFilter != null) {
            http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }

    /**
     * ✅ Hybrid JWT Decoder:
     *  - Primary: Keycloak RS256 (via JWKS endpoint)
     *  - Fallback: Local HS256 for internal tokens
     */
    @Bean
    public JwtDecoder combinedJwtDecoder() {
        try {
            // ✅ Try loading Keycloak’s RS256 public keys
            System.out.println("✅ Initializing Keycloak JWT Decoder from issuer: " + issuerUri);
            return JwtDecoders.fromIssuerLocation(issuerUri);
        } catch (Exception e) {
            // ⚠️ Fallback to local JWT (HS256)
            System.out.println("⚠️ Failed to load Keycloak JWKS, using local JWT decoder: " + e.getMessage());
            SecretKeySpec secretKey = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            return NimbusJwtDecoder.withSecretKey(secretKey).build();
        }
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * ✅ Global CORS for local + staging + production
     */
    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("http://localhost:3000"); // Local frontend
        config.addAllowedOriginPattern("https://aran-stg.zpoa.com"); // Keycloak
        config.addAllowedOriginPattern("https://portal.ciyex.com");  // Production
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
