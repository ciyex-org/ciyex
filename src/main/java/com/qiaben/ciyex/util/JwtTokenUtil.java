package com.qiaben.ciyex.util;

import com.qiaben.ciyex.entity.Org;
import com.qiaben.ciyex.entity.Role;
import com.qiaben.ciyex.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class JwtTokenUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpirationInMs;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate JWT for the given user, including all orgs and roles.
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("email", user.getEmail());
        claims.put("fullName", user.getFullName());

        // Add organizations as a list of orgId/orgName maps
        List<Map<String, Object>> orgList = user.getOrgs().stream()
                .map(org -> {
                    Map<String, Object> orgMap = new HashMap<>();
                    orgMap.put("orgId", org.getId());
                    orgMap.put("orgName", org.getOrgName());
                    return orgMap;
                })
                .collect(Collectors.toList());
        claims.put("orgs", orgList);

        // Add roles as a list of strings
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList());
        claims.put("roles", roles);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationInMs))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Validate token against given email.
     */
    public boolean validateToken(String token, String email) {
        String tokenEmail = getEmailFromToken(token);
        return (email.equals(tokenEmail) && !isTokenExpired(token));
    }

    public String getEmailFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public Long getUserIdFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.get("userId", Long.class));
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getOrgsFromToken(String token) {
        return getClaimFromToken(token, claims -> (List<Map<String, Object>>) claims.get("orgs"));
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        return getClaimFromToken(token, claims -> (List<String>) claims.get("roles"));
    }

    private Boolean isTokenExpired(String token) {
        Date expiration = getClaimFromToken(token, Claims::getExpiration);
        return expiration.before(new Date());
    }

    private <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getBody();
        return claimsResolver.apply(claims);
    }
}
