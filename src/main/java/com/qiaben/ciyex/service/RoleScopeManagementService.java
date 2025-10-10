package com.qiaben.ciyex.service;

import com.qiaben.ciyex.auth.scope.Scope;
import com.qiaben.ciyex.auth.scope.ScopeRepository;
import com.qiaben.ciyex.auth.scope.RoleScopeTemplateRepository;
import com.qiaben.ciyex.auth.scope.RoleScopeTemplate;
import com.qiaben.ciyex.entity.RoleName;
import com.qiaben.ciyex.entity.User;
import com.qiaben.ciyex.entity.UserOrgRole;
import com.qiaben.ciyex.repository.UserOrgRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleScopeManagementService {

    private final ScopeRepository scopeRepository;
    private final RoleScopeTemplateRepository roleScopeTemplateRepository;
    private final UserOrgRoleRepository userOrgRoleRepository;

    /**
     * Default scope assignments for each role
     * This is the single source of truth for role-based permissions
     */
    private static final Map<RoleName, List<String>> DEFAULT_ROLE_SCOPES = Map.of(
        // SUPER_ADMIN gets all scopes (handled separately)
        RoleName.ADMIN, Arrays.asList(
            "user:read", "user:write", 
            "patients:read", "patients:write",
            "appointments:read", "appointments:write",
            "messaging:read", "messaging:write",
            "labs:read", "labs:write"
        ),
        
        RoleName.PROVIDER, Arrays.asList(
            "user:read", "user:write",
            "patients:read", "patients:write", 
            "appointments:read", "appointments:write",
            "messaging:read", "messaging:write",
            "labs:read", "labs:write"
        ),
        
        RoleName.NURSE, Arrays.asList(
            "user:read",
            "patients:read",
            "appointments:read", "appointments:write",
            "messaging:read", "messaging:write",
            "labs:read"
        ),
        
        RoleName.PATIENT, Arrays.asList(
            "user:read", "user:write",
            "appointments:read",
            "messaging:read", "messaging:write",
            "labs:read"
        ),
        
        RoleName.RECEPTIONIST, Arrays.asList(
            "user:read",
            "patients:read",
            "appointments:read", "appointments:write",
            "messaging:read"
        ),
        
        RoleName.BILLER, Arrays.asList(
            "user:read",
            "patients:read",
            "appointments:read",
            "messaging:read",
            "labs:read"
        )
    );

    /**
     * Get default scopes for a specific role
     */
    public List<String> getDefaultScopesForRole(RoleName role) {
        if (role == RoleName.SUPER_ADMIN) {
            // Super admin gets all active scopes
            return scopeRepository.findAll().stream()
                    .filter(Scope::isActive)
                    .map(Scope::getCode)
                    .collect(Collectors.toList());
        }
        
        return DEFAULT_ROLE_SCOPES.getOrDefault(role, Arrays.asList("user:read"));
    }

    /**
     * Initialize role-scope templates in database
     * Call this during application startup or when adding new roles
     */
    @Transactional
    public void initializeRoleScopeTemplates() {
        log.info("Initializing role-scope templates...");
        
        for (Map.Entry<RoleName, List<String>> entry : DEFAULT_ROLE_SCOPES.entrySet()) {
            RoleName role = entry.getKey();
            List<String> scopeCodes = entry.getValue();
            
            log.info("Setting up default scopes for role: {}", role);
            
            for (String scopeCode : scopeCodes) {
                Optional<Scope> scopeOpt = scopeRepository.findByCode(scopeCode);
                if (scopeOpt.isPresent()) {
                    Scope scope = scopeOpt.get();
                    
                    // Check if template already exists
                    if (!roleScopeTemplateRepository.existsByRoleAndScope(role, scope)) {
                        RoleScopeTemplate template = new RoleScopeTemplate();
                        template.setRole(role);
                        template.setScope(scope);
                        
                        try {
                            roleScopeTemplateRepository.save(template);
                            log.debug("Created template: {} -> {}", role, scopeCode);
                        } catch (Exception e) {
                            log.warn("Failed to create template for {}->{}: {}", role, scopeCode, e.getMessage());
                            // Continue with next scope instead of failing completely
                        }
                    }
                } else {
                    log.warn("Scope not found: {}", scopeCode);
                }
            }
        }
        
        log.info("Role-scope template initialization completed");
    }

    /**
     * Add a new role with default scopes
     * Use this method when introducing new roles to the system
     */
    @Transactional
    public void addNewRole(RoleName newRole, List<String> defaultScopes) {
        log.info("Adding new role: {} with default scopes: {}", newRole, defaultScopes);
        
        for (String scopeCode : defaultScopes) {
            Optional<Scope> scopeOpt = scopeRepository.findByCode(scopeCode);
            if (scopeOpt.isPresent()) {
                Scope scope = scopeOpt.get();
                
                if (!roleScopeTemplateRepository.existsByRoleAndScope(newRole, scope)) {
                    RoleScopeTemplate template = new RoleScopeTemplate();
                    template.setRole(newRole);
                    template.setScope(scope);
                    
                    roleScopeTemplateRepository.save(template);
                    log.info("Added scope {} to role {}", scopeCode, newRole);
                }
            } else {
                log.error("Cannot add scope {} to role {} - scope does not exist", scopeCode, newRole);
            }
        }
    }

    /**
     * Get all scopes for a user in a specific organization
     * This combines:
     * 1. Default role-based scopes from templates
     * 2. Additional user-specific scopes (if any)
     */
    public Set<String> getUserScopesForOrg(User user, Long orgId) {
        Set<String> allScopes = new HashSet<>();
        
        // Get user's roles for this org
        List<UserOrgRole> userOrgRoles = user.getUserOrgRoles().stream()
                .filter(uor -> uor.getOrg().getId().equals(orgId))
                .collect(Collectors.toList());
        
        for (UserOrgRole userOrgRole : userOrgRoles) {
            RoleName role = userOrgRole.getRole();
            
            if (role == RoleName.SUPER_ADMIN) {
                // Super admin gets all scopes
                return scopeRepository.findAll().stream()
                        .filter(Scope::isActive)
                        .map(Scope::getCode)
                        .collect(Collectors.toSet());
            }
            
            // Add default role scopes
            List<String> defaultScopes = getDefaultScopesForRole(role);
            allScopes.addAll(defaultScopes);
            
            // Add template-based scopes (from database)
            List<Scope> templateScopes = roleScopeTemplateRepository.findScopesByRole(role);
            allScopes.addAll(templateScopes.stream()
                    .filter(Scope::isActive)
                    .map(Scope::getCode)
                    .collect(Collectors.toList()));
        }
        
        // TODO: Add user-specific additional scopes if you implement user_additional_scopes table
        // allScopes.addAll(getUserAdditionalScopes(user.getId(), orgId));
        
        return allScopes;
    }

    /**
     * Assign additional scope to a specific user in an organization
     * This is for cases where you want to give extra permissions beyond role defaults
     */
    @Transactional
    public void assignAdditionalScopeToUser(Long userId, Long orgId, String scopeCode) {
        // TODO: Implement user_additional_scopes table if needed
        // This would store user-specific scope overrides beyond role defaults
        log.info("TODO: Implement additional scope assignment for user {} in org {} with scope {}", 
                userId, orgId, scopeCode);
    }

    /**
     * Remove additional scope from a specific user in an organization
     */
    @Transactional 
    public void removeAdditionalScopeFromUser(Long userId, Long orgId, String scopeCode) {
        // TODO: Implement user_additional_scopes table if needed
        log.info("TODO: Implement additional scope removal for user {} in org {} with scope {}", 
                userId, orgId, scopeCode);
    }

    /**
     * Get detailed scope information for debugging/admin purposes
     */
    public Map<String, Object> getScopeAnalysis(User user, Long orgId) {
        Map<String, Object> analysis = new HashMap<>();
        
        List<UserOrgRole> userOrgRoles = user.getUserOrgRoles().stream()
                .filter(uor -> uor.getOrg().getId().equals(orgId))
                .collect(Collectors.toList());
        List<String> roles = userOrgRoles.stream()
                .map(uor -> uor.getRole().name())
                .collect(Collectors.toList());
        
        Set<String> allScopes = getUserScopesForOrg(user, orgId);
        
        analysis.put("userId", user.getId());
        analysis.put("orgId", orgId);
        analysis.put("roles", roles);
        analysis.put("totalScopes", allScopes.size());
        analysis.put("scopes", allScopes);
        analysis.put("isSuperAdmin", roles.contains("SUPER_ADMIN"));
        
        return analysis;
    }
}