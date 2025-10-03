package com.qiaben.ciyex.auth.scope;

import com.qiaben.ciyex.entity.RoleName;
import com.qiaben.ciyex.repository.UserOrgRoleRepository;
import com.qiaben.ciyex.service.RoleScopeManagementService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class ScopeSeeder implements CommandLineRunner {

    private final ScopeRepository scopeRepository;
    private final RoleScopeTemplateRepository templateRepository;
    private final UserOrgRoleRepository userOrgRoleRepository;
    private final JdbcTemplate jdbcTemplate;
    private final RoleScopeManagementService roleScopeManagementService;

    @Override
    public void run(String... args) throws Exception {
        try {
            log.info("Starting scope seeding...");
            Map<String, Object> result = seed();
            log.info("Scope seeding completed successfully: {}", result);
            
            // Initialize role-scope templates using the new service
            log.info("Initializing role-scope templates using RoleScopeManagementService...");  
            try {
                roleScopeManagementService.initializeRoleScopeTemplates();
                log.info("Role-scope template initialization completed successfully");
            } catch (Exception e) {
                log.warn("Role-scope template initialization encountered issues but continuing: {}", e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("Error during scope seeding", e);
            throw e;
        }
    }

    @Transactional
    public Map<String, Object> seed() {
        // 0) Make the role CHECK constraint match real roles, and clean conflicting rows first
        syncRoleCheckConstraintWithUserRoles();

        int createdScopes = 0;
        int existingScopes = 0;
        int createdTemplates = 0;

    // 1) Seed master scopes (idempotent)
    Map<String, String> scopes = Map.ofEntries(
        Map.entry("user:read", "User Read"),
        Map.entry("user:write", "User Write"),
        Map.entry("appointments:read", "Appointments Read"),
        Map.entry("appointments:write", "Appointments Write"),
        Map.entry("messaging:read", "Messaging Read"),
        Map.entry("messaging:write", "Messaging Write"),
        Map.entry("labs:read", "Labs Read"),
        Map.entry("labs:write", "Labs Write"),
        Map.entry("patients:read", "Patients Read"),
        Map.entry("patients:write", "Patients Write")
    );
        for (var e : scopes.entrySet()) {
            String code = e.getKey();
            var opt = scopeRepository.findByCode(code);
            if (opt.isEmpty()) {
                Scope s = new Scope();
                s.setCode(code);
                s.setName(e.getValue());
                s.setDescription(e.getValue());
                s.setActive(true);
                try {
                    s.getClass().getMethod("setCreatedAt", Instant.class).invoke(s, Instant.now());
                    s.getClass().getMethod("setUpdatedAt", Instant.class).invoke(s, Instant.now());
                } catch (Exception ignore) {}
                scopeRepository.save(s);
                createdScopes++;
            } else {
                existingScopes++;
            }
        }

        // 2) Which roles actually exist in user_org_roles?
        List<RoleName> rolesInDb = userOrgRoleRepository.findDistinctRoles();

        // 3) For each role (except SUPER_ADMIN), seed default templates
        List<Scope> allScopes = scopeRepository.findAll();
        Set<String> allCodes = allScopes.stream().map(Scope::getCode).collect(Collectors.toCollection(LinkedHashSet::new));

        for (RoleName role : rolesInDb) {
            if (role == null) continue;
            if (role.name().equals("SUPER_ADMIN")) continue; // super-admin is implicit, never stored

            List<String> defaultCodes = defaultsFor(role.name(), allCodes);
            for (String code : defaultCodes) {
                var opt = scopeRepository.findByCode(code);
                if (opt.isPresent()) {
                    Scope s = opt.get();
                    if (!templateRepository.existsByRoleAndScope(role, s)) {
                        try {
                            RoleScopeTemplate t = new RoleScopeTemplate();
                            t.setRole(role);
                            t.setScope(s);
                            templateRepository.save(t);
                            createdTemplates++;
                        } catch (RuntimeException ex) {
                            if (isRoleConstraint(ex)) {
                                // If the DB still complains about the role constraint, skip this pair
                            } else {
                                throw ex;
                            }
                        }
                    }
                }
            }
        }

        return Map.of(
                "createdScopes", createdScopes,
                "existingScopes", existingScopes,
                "createdTemplates", createdTemplates,
                "rolesSeen", rolesInDb.stream().map(Enum::name).toList()
        );
    }

    /** Build default scope set for a role name (string), filtered to codes that actually exist. */
    private List<String> defaultsFor(String roleName, Collection<String> allCodes) {
        List<String> raw;
        if ("ADMIN".equals(roleName)) {
            raw = new ArrayList<>(allCodes); // ADMIN → all master scopes
        } else if ("DOCTOR".equals(roleName) || "CLINICIAN".equals(roleName)
                || "PROVIDER".equals(roleName) || "PHYSICIAN".equals(roleName)
                || "NURSE".equals(roleName)) {
        raw = List.of("user:read","appointments:read","appointments:write",
            "messaging:read","messaging:write","labs:read","labs:write","patients:read");
        } else if ("PATIENT".equals(roleName) || "MEMBER".equals(roleName)) {
            raw = List.of("user:read","appointments:read","messaging:read","messaging:write","labs:read");
        } else if ("BILLER".equals(roleName) || "RECEPTIONIST".equals(roleName)) {
            raw = List.of("user:read","appointments:read","messaging:read","labs:read","patients:read");
        } else {
            // fallback for any other role
            raw = List.of("user:read");
        }
        return raw.stream().filter(allCodes::contains).distinct().collect(Collectors.toList());
    }

    /** True if the exception chain mentions the role check constraint. */
    private boolean isRoleConstraint(Throwable t) {
        while (t != null) {
            String m = t.getMessage();
            if (m != null && m.contains("auth_role_scope_template_role_check")) return true;
            t = t.getCause();
        }
        return false;
    }

    /**
     * Ensure the auth_role_scope_template.role CHECK constraint matches all possible role values
     * from RoleName enum (minus SUPER_ADMIN), and delete any template rows that would violate it.
     */
    private void syncRoleCheckConstraintWithUserRoles() {
        // Get all possible roles from RoleName enum (except SUPER_ADMIN)
        LinkedHashSet<String> allowed = Arrays.stream(RoleName.values())
                .filter(role -> role != RoleName.SUPER_ADMIN)
                .map(Enum::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // drop existing constraint (if any)
        jdbcTemplate.execute(
                "ALTER TABLE auth_role_scope_template " +
                        "DROP CONSTRAINT IF EXISTS auth_role_scope_template_role_check"
        );

        // delete template rows that are out-of-policy (role not in allowed) or SUPER_ADMIN rows
        if (allowed.isEmpty()) {
            jdbcTemplate.execute("DELETE FROM auth_role_scope_template");
        } else {
            String inList = allowed.stream()
                    .map(r -> "'" + r.replace("'", "''") + "'")
                    .collect(Collectors.joining(","));
            jdbcTemplate.execute(
                    "DELETE FROM auth_role_scope_template " +
                            "WHERE role = 'SUPER_ADMIN' OR role NOT IN (" + inList + ")"
            );
        }

        // re-add the check constraint
        if (allowed.isEmpty()) {
            // no roles known yet → keep a trivial check
            jdbcTemplate.execute(
                    "ALTER TABLE auth_role_scope_template " +
                            "ADD CONSTRAINT auth_role_scope_template_role_check CHECK (role IS NOT NULL)"
            );
        } else {
            String inList = allowed.stream()
                    .map(r -> "'" + r.replace("'", "''") + "'")
                    .collect(Collectors.joining(","));
            jdbcTemplate.execute(
                    "ALTER TABLE auth_role_scope_template " +
                            "ADD CONSTRAINT auth_role_scope_template_role_check " +
                            "CHECK (role IN (" + inList + "))"
            );
        }
    }
}
