
package com.qiaben.ciyex.filter;

import com.qiaben.ciyex.service.CiyexUserDetailsService;
import com.qiaben.ciyex.service.KeycloakAuthService;
import com.qiaben.ciyex.util.JwtTokenUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtRequestFilter.class);

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private CiyexUserDetailsService userDetailsService;
    
    @Autowired
    private KeycloakAuthService keycloakAuthService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {

        // 1) Allow CORS preflight without auth
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");

        String token = null;
        String username = null;
        boolean isKeycloakToken = false;

        // 2) Extract Bearer token if present
        if (authHeader != null) {
            if (authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
                try {
                    // Try to parse as internal JWT first
                    username = jwtTokenUtil.getEmailFromToken(token);
                } catch (Exception e) {
                    // If internal JWT parsing fails, try Keycloak token
                    try {
                        username = keycloakAuthService.extractEmailFromToken(token);
                        isKeycloakToken = true;
                        log.debug("Keycloak token detected for user: {}", username);
                    } catch (Exception ex) {
                        log.debug("JWT parse error for both internal and Keycloak tokens", ex);
                    }
                }
            } else {
                // Header present but not Bearer ⇒ don't spam WARN; just debug
                log.debug("Authorization header present but not Bearer");
            }
        }

        // 3) Validate + set Authentication only if not already set
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                if (isKeycloakToken) {
                    // For Keycloak tokens, create a simple authentication with groups as authorities
                    List<String> groups = keycloakAuthService.extractGroupsFromToken(token);
                    List<GrantedAuthority> authorities = groups.stream()
                            .map(group -> (GrantedAuthority) new SimpleGrantedAuthority(group))
                            .collect(Collectors.toList());
                    
                    // Create a simple user principal for Keycloak authenticated users
                    org.springframework.security.core.userdetails.User keycloakUser = 
                        new org.springframework.security.core.userdetails.User(username, "", authorities);
                    
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(keycloakUser, null, authorities);
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.debug("Keycloak authentication set for user: {}", username);
                } else {
                    // Internal JWT validation
                    UserDetails user = userDetailsService.loadUserByUsername(username);

                    // If your util validates with username string (current behavior)
                    boolean valid = jwtTokenUtil.validateToken(token, user.getUsername());

                    // If you have/choose to add an overload: validateToken(token, user)
                    // boolean valid = jwtTokenUtil.validateToken(token, user);

                    if (valid) {
                        // Prefer authorities from DB; if you put roles in the token, you could merge them here.
                        Collection<? extends GrantedAuthority> normalized =
                                ensureRolePrefix(user.getAuthorities());

                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(user, null, normalized);

                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    } else {
                        log.debug("JWT validation failed for user {}", username);
                    }
                }
            } catch (Exception e) {
                log.debug("Authentication setup failed", e);
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * Ensure authorities have ROLE_ prefix so hasRole('X') checks succeed.
     * If your UserDetails already returns ROLE_* authorities, this is a no-op.
     */
    private Collection<? extends GrantedAuthority> ensureRolePrefix(Collection<? extends GrantedAuthority> authorities) {
        if (authorities == null) return List.of();
        return authorities.stream()
                .map(a -> {
                    String name = a.getAuthority();
                    String withPrefix = name.startsWith("ROLE_") ? name : "ROLE_" + name;
                    return (GrantedAuthority) () -> withPrefix;
                })
                .collect(Collectors.toList());
    }
}
