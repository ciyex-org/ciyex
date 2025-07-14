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
     * Generate JWT for the given user, including all orgs, facilities, and roles.
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("email", user.getEmail());
        claims.put("fullName", user.getFullName());

        // --- New nested orgs -> facilities -> roles structure ---
        // Group userFacilityRoles by Org -> Facility
        Map<Long, Map<String, Object>> orgMap = new LinkedHashMap<>();

        Set<Long> facilityIdSet = new HashSet<>();

        for (UserFacilityRole ufr : user.getUserFacilityRoles()) {
            Facility facility = ufr.getFacility();
            Org org = facility.getOrg();

            // Build or fetch org entry
            Map<String, Object> orgEntry = orgMap.computeIfAbsent(org.getId(), k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("orgId", org.getId());
                m.put("orgName", org.getOrgName());
                m.put("facilities", new LinkedHashMap<Long, Map<String, Object>>());
                return m;
            });

            @SuppressWarnings("unchecked")
            Map<Long, Map<String, Object>> facilityMap = (Map<Long, Map<String, Object>>) orgEntry.get("facilities");

            // Build or fetch facility entry
            Map<String, Object> facilityEntry = facilityMap.computeIfAbsent(facility.getId(), k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("facilityId", facility.getId());
                m.put("facilityName", facility.getFacilityName());
                m.put("roles", new LinkedHashSet<String>());
                return m;
            });

            @SuppressWarnings("unchecked")
            Set<String> roles = (Set<String>) facilityEntry.get("roles");
            roles.add(ufr.getRole().name());

            facilityIdSet.add(facility.getId());
        }

        // Convert org map to org list, each with facilities as a list (not map)
        List<Map<String, Object>> orgList = orgMap.values().stream()
                .map(orgEntry -> {
                    Map<Long, Map<String, Object>> facilityMap = (Map<Long, Map<String, Object>>) orgEntry.remove("facilities");
                    List<Map<String, Object>> facilities = facilityMap.values().stream()
                            .peek(facEntry -> {
                                // Convert roles set to list
                                Set<String> roles = (Set<String>) facEntry.get("roles");
                                facEntry.put("roles", new ArrayList<>(roles));
                            })
                            .collect(Collectors.toList());
                    orgEntry.put("facilities", facilities);
                    return orgEntry;
                })
                .collect(Collectors.toList());

        claims.put("orgs", orgList);

        // Optional: Add all facilityIds as a flat array
        claims.put("facilityIds", new ArrayList<>(facilityIdSet));

        // Optional: Add all orgIds as a flat array
        claims.put("orgIds", orgList.stream().map(o -> o.get("orgId")).collect(Collectors.toList()));

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
    public List<Long> getOrgIdsFromToken(String token) {
        return getClaimFromToken(token, claims -> (List<Long>) claims.get("orgIds"));
    }

    @SuppressWarnings("unchecked")
    public List<Long> getFacilityIdsFromToken(String token) {
        return getClaimFromToken(token, claims -> (List<Long>) claims.get("facilityIds"));
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
