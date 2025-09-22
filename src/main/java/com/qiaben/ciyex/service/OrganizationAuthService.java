package com.qiaben.ciyex.service;

import com.qiaben.ciyex.entity.Org;
import com.qiaben.ciyex.entity.User;
import com.qiaben.ciyex.entity.UserOrgRole;
import com.qiaben.ciyex.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationAuthService {
    
    private final UserRepository userRepository;
    
    /**
     * Get organization ID for a user during login
     * This method queries the master database (ciyexdb) to find which organization(s) 
     * the user belongs to
     */
    public Long getOrganizationIdForUser(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            log.warn("User not found with email: {}", email);
            return null;
        }
        
        User user = userOpt.get();
        Set<UserOrgRole> userOrgRoles = user.getUserOrgRoles();
        
        if (userOrgRoles.isEmpty()) {
            log.warn("User {} has no organization roles", email);
            return null;
        }
        
        // For now, return the first organization ID
        // In a more complex scenario, you might want to handle multiple orgs differently
        UserOrgRole firstRole = userOrgRoles.iterator().next();
        Long orgId = firstRole.getOrg().getId();
        
        log.info("Found organization ID {} for user {}", orgId, email);
        return orgId;
    }
    
    /**
     * Get all organizations a user has access to
     */
    public List<Org> getOrganizationsForUser(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return List.of();
        }
        
        return userOpt.get().getUserOrgRoles().stream()
                .map(UserOrgRole::getOrg)
                .collect(Collectors.toList());
    }
    
    /**
     * Validate if a user has access to a specific organization
     */
    public boolean validateUserOrgAccess(String email, Long orgId) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return false;
        }
        
        return userOpt.get().getUserOrgRoles().stream()
                .anyMatch(role -> role.getOrg().getId().equals(orgId));
    }
    
    /**
     * Get the default organization for a user (first one in the list)
     */
    public Long getDefaultOrganizationForUser(String email) {
        return getOrganizationIdForUser(email);
    }
}
