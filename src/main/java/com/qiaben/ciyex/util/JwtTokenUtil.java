package com.qiaben.ciyex.util;

import com.qiaben.ciyex.entity.*;
import com.qiaben.ciyex.auth.scope.Scope;
import com.qiaben.ciyex.auth.scope.ScopeRepository;
import com.qiaben.ciyex.auth.scope.UserScopeService;
import com.qiaben.ciyex.auth.scope.RoleScopeTemplateRepository;
import com.qiaben.ciyex.security.SuperAdminConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private UserScopeService userScopeService;

    @Autowired
    private ScopeRepository scopeRepository;

    @Autowired
    private RoleScopeTemplateRepository roleScopeTemplateRepository;

    @Autowired
    private SuperAdminConfig superAdminConfig;
    
    @Autowired
    private com.qiaben.ciyex.service.RoleScopeManagementService roleScopeManagementService;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate JWT for the given user, including orgs, roles, and scopes.
     * Super admin gets implicit full access (all active scopes) and "isSuperAdmin": true.
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("email", user.getEmail());
        claims.put("sub", user.getEmail());
        claims.put("firstName", user.getFirstName());
        claims.put("lastName", user.getLastName());
        claims.put("uuid", user.getUuid());

        // === Scopes ===
        List<String> scopes;
        boolean isSa = superAdminConfig != null && superAdminConfig.isSuperAdmin(user);
        if (isSa) {
            scopes = scopeRepository.findAll().stream()
                    .filter(Scope::isActive)
                    .map(Scope::getCode)
                    .toList();
            claims.put("isSuperAdmin", true);
        } else {
            scopes = userScopeService.getActiveScopeCodesByUserId(user.getId());
            claims.put("isSuperAdmin", false);
        }
    // Do not emit a global 'scope' claim; scopes are assigned per-org only.

        // === Per-org roles and scopes ===
        Map<Long, Map<String, Object>> orgsMap = new LinkedHashMap<>();
        for (UserOrgRole uor : user.getUserOrgRoles()) {
            Org org = uor.getOrg();
            Map<String, Object> orgEntry = orgsMap.computeIfAbsent(org.getId(), k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("orgId", org.getId());
                m.put("orgName", org.getOrgName());
                m.put("roles", new LinkedHashSet<String>());
                m.put("scopes", new LinkedHashSet<String>());
                return m;
            });
            @SuppressWarnings("unchecked")
            Set<String> roles = (Set<String>) orgEntry.get("roles");
            @SuppressWarnings("unchecked")
            Set<String> orgScopes = (Set<String>) orgEntry.get("scopes");
            roles.add(uor.getRole().name());
            
            // Use the new RoleScopeManagementService to get all scopes for user in this org
            // This includes both default role scopes and any additional user-specific scopes
            Set<String> userOrgScopes = roleScopeManagementService.getUserScopesForOrg(user, org.getId());
            orgScopes.addAll(userOrgScopes);
        }

        // Build org list and only keep scopes that the user actually has
        List<Map<String, Object>> orgList = new ArrayList<>();
        Set<String> userScopeSet = new LinkedHashSet<>(scopes);
        for (Map<String, Object> orgEntry : orgsMap.values()) {
            Object rolesObj = orgEntry.get("roles");
            Object scopesObj = orgEntry.get("scopes");
            List<String> rolesList = new ArrayList<>();
            List<String> orgScopesList = new ArrayList<>();
            if (rolesObj instanceof Set) {
                for (Object r : (Set<?>) rolesObj) rolesList.add(String.valueOf(r));
            }
            if (scopesObj instanceof Set) {
                for (Object s : (Set<?>) scopesObj) {
                    String code = String.valueOf(s);
                    if (userScopeSet.contains(code)) orgScopesList.add(code);
                }
            }
            Map<String, Object> copy = new LinkedHashMap<>(orgEntry);
            copy.put("roles", rolesList);
            // Emit org scopes as a single space-delimited string (empty string if none)
            copy.put("scopes", String.join(" ", orgScopesList));
            orgList.add(copy);
        }

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

    // === Existing methods unchanged ===

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
