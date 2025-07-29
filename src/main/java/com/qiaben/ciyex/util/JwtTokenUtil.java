package com.qiaben.ciyex.util;

import com.qiaben.ciyex.entity.*;
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
     * Generate JWT for the given user, including all orgs and roles. No facilities.
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("email", user.getEmail());
        claims.put("sub", user.getEmail());
        claims.put("firstName", user.getFirstName());
        claims.put("lastName", user.getLastName());
        claims.put("uuid", user.getUuid());

        // Group roles by org
        Map<Long, Map<String, Object>> orgsMap = new LinkedHashMap<>();
        for (UserOrgRole uor : user.getUserOrgRoles()) {
            Org org = uor.getOrg();
            Map<String, Object> orgEntry = orgsMap.computeIfAbsent(org.getId(), k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("orgId", org.getId());
                m.put("orgName", org.getOrgName());
                m.put("roles", new LinkedHashSet<String>());
                return m;
            });
            @SuppressWarnings("unchecked")
            Set<String> roles = (Set<String>) orgEntry.get("roles");
            roles.add(uor.getRole().name());
        }

        // Convert roles set to list for JSON
        List<Map<String, Object>> orgList = orgsMap.values().stream()
                .peek(orgEntry -> {
                    Set<String> roles = (Set<String>) orgEntry.get("roles");
                    orgEntry.put("roles", new ArrayList<>(roles));
                })
                .collect(Collectors.toList());

        claims.put("orgs", orgList);
        claims.put("orgIds", orgList.stream().map(o -> o.get("orgId")).collect(Collectors.toList()));

        long nowSec = System.currentTimeMillis() / 1000;
        long expSec = nowSec + (jwtExpirationInMs / 1000);
        claims.put("iat", nowSec);
        claims.put("exp", expSec);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getEmail())
                .setIssuedAt(new Date(nowSec * 1000))
                .setExpiration(new Date(expSec * 1000))
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
    public List<Long> getOrgIdsFromToken(String token) {
        return getClaimFromToken(token, claims -> (List<Long>) claims.get("orgIds"));
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
