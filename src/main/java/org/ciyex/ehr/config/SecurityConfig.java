package org.ciyex.ehr.config;

import org.ciyex.ehr.security.KeycloakJwtAuthenticationConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${cors.allowed-origins:http://localhost:*}")
    private String corsAllowedOrigins;

    public SecurityConfig(KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter) {
        this.keycloakJwtAuthenticationConverter = keycloakJwtAuthenticationConverter;
    }

    /**
     * Higher-priority filter chain for fully public endpoints.
     * No OAuth2 resource server — avoids BearerTokenAuthenticationFilter interference.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain publicFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(
                "/api/portal/orgs/**",
                "/api/portal/auth/**",
                "/api/portal/approvals/**",
                "/api/auth/**",
                "/api/public/**",
                "/api/internal/**",
                "/actuator/**"
            )
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/**",
                    "/api/public/**",
                    "/api/portal/auth/**",
                    "/api/portal/approvals/**",
                    "/api/portal/orgs/**",
                    "/api/internal/**",
                    "/actuator/**"
                ).permitAll()
                // Fine-grained access control is enforced via SMART on FHIR
                // @PreAuthorize annotations on controllers (enabled by @EnableMethodSecurity).
                .requestMatchers("/api/admin/**").authenticated()
                .requestMatchers("/api/app-installations/**").authenticated()
                .requestMatchers("/api/app-context/**").authenticated()
                .requestMatchers("/api/smart-launch/**").authenticated()
                .requestMatchers("/api/app-usage/**").authenticated()
                .requestMatchers("/api/provider/**").authenticated()
                .requestMatchers("/api/portal/**").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth -> oauth
                .jwt(jwt -> jwt
                    .decoder(combinedJwtDecoder())
                    .jwtAuthenticationConverter(keycloakJwtAuthenticationConverter)
                )
            );

        return http.build();
    }

    @Bean
    public JwtDecoder combinedJwtDecoder() {
        // Portal JWTs are signed with HS384 using the base64-decoded jwt.secret
        // (PortalAuthService uses Keys.hmacShaKeyFor(Base64.decode(jwtSecret))
        //  which auto-selects HS384 when key >= 48 bytes).
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "HmacSHA384");
        NimbusJwtDecoder localDecoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS384)
                .build();
        
        final NimbusJwtDecoder[] keycloakDecoder = {null};
        
        return token -> {
            if (keycloakDecoder[0] == null) {
                try {
                    keycloakDecoder[0] = (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(issuerUri);
                } catch (Exception e) {
                    // Keycloak unavailable, use local only
                }
            }
            
            if (keycloakDecoder[0] != null) {
                try {
                    return keycloakDecoder[0].decode(token);
                } catch (Exception e) {
                    // Fall through to local
                }
            }
            
            return localDecoder.decode(token);
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        for (String origin : corsAllowedOrigins.split(",")) {
            config.addAllowedOriginPattern(origin.trim());
        }
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
