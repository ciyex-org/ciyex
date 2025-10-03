package com.qiaben.ciyex.auth.scope;

import com.qiaben.ciyex.entity.RoleName; // ✅ use your real role enum
import jakarta.transaction.Transactional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.*;

@Configuration
public class ScopeBootstrap {

    @Bean
    @Transactional
    public Runnable seedScopesAndTemplates(
            ScopeRepository scopeRepository,
            RoleScopeTemplateRepository templateRepository
    ) {
        return () -> {
            // 1) Seed master scopes (idempotent)
        Map<String, String> scopes = Map.ofEntries(
            Map.entry("user:read",          "User Read"),
            Map.entry("user:write",         "User Write"),
            Map.entry("appointments:read",  "Appointments Read"),
            Map.entry("appointments:write", "Appointments Write"),
            Map.entry("messaging:read",     "Messaging Read"),
            Map.entry("messaging:write",    "Messaging Write"),
            Map.entry("labs:read",          "Labs Read"),
            Map.entry("labs:write",         "Labs Write"),
            Map.entry("patients:read",      "Patients Read"),
            Map.entry("patients:write",     "Patients Write")
        );

            for (var e : scopes.entrySet()) {
                String code = e.getKey();
                if (!scopeRepository.existsByCode(code)) {
                    Scope s = new Scope();
                    s.setCode(code);
                    s.setName(e.getValue());
                    s.setDescription(e.getValue());
                    s.setActive(true);
                    try {
                        s.getClass().getMethod("setCreatedAt", Instant.class).invoke(s, Instant.now());
                        s.getClass().getMethod("setUpdatedAt", Instant.class).invoke(s, Instant.now());
                    } catch (Exception ignore) { /* fields may already default */ }
                    scopeRepository.save(s);
                }
            }

            // 2) Seed role → default scope templates (idempotent)
            // Use strings to avoid compile errors if a constant (e.g., DOCTOR) doesn't exist in RoleName
            Map<String, List<String>> roleMatrix = new LinkedHashMap<>();
            roleMatrix.put("ADMIN", null); // ADMIN → all scopes
        roleMatrix.put("DOCTOR", List.of(
            "user:read", "appointments:read", "appointments:write",
            "messaging:read", "messaging:write",
            "labs:read", "labs:write", "patients:read"
        ));
        roleMatrix.put("PATIENT", List.of(
            "user:read", "appointments:read", "messaging:read", "messaging:write", "labs:read"
        ));
            // NOTE: SUPER_ADMIN intentionally omitted (implicit access elsewhere)

            List<Scope> all = scopeRepository.findAll();

            for (var entry : roleMatrix.entrySet()) {
                String roleKey = entry.getKey();
                RoleName roleEnum;
                try {
                    roleEnum = RoleName.valueOf(roleKey);  // only if present in your enum
                } catch (IllegalArgumentException ex) {
                    continue; // this role name doesn't exist in RoleName → skip cleanly
                }

                if ("ADMIN".equals(roleKey)) {
                    for (Scope s : all) {
                        ensureTemplate(templateRepository, roleEnum, s);
                    }
                } else {
                    List<String> codes = entry.getValue();
                    if (codes == null) continue;
                    for (String code : codes) {
                        scopeRepository.findByCode(code)
                                .ifPresent(s -> ensureTemplate(templateRepository, roleEnum, s));
                    }
                }
            }
        };
    }

    private static void ensureTemplate(RoleScopeTemplateRepository repo, RoleName role, Scope scope) {
        // If your repo has existsByRoleAndScope(role, scope), you can check before saving.
        try {
            RoleScopeTemplate t = new RoleScopeTemplate();
            t.setRole(role);
            t.setScope(scope);
            repo.save(t); // unique constraint prevents duplicates
        } catch (Exception ignore) {
            // Already exists (unique constraint)
        }
    }
}
