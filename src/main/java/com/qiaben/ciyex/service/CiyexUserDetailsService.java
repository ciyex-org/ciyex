package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.core.integration.RequestContext;
import com.qiaben.ciyex.entity.*;
import com.qiaben.ciyex.repository.OrgRepository;
import com.qiaben.ciyex.repository.UserRepository;
import com.qiaben.ciyex.repository.UserOrgRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CiyexUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OrgRepository orgRepository;
    @Autowired
    private UserOrgRoleRepository userOrgRoleRepository;

    // Used by Spring Security for login; orgId is OPTIONAL here
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // Get orgId from context if present, else allow login for all orgs
        RequestContext ctx = RequestContext.get();
        Long orgId = ctx != null ? ctx.getOrgId() : null;

        Set<UserOrgRole> roles;
        if (orgId != null) {
            // If orgId present, filter by org
            roles = user.getUserOrgRoles().stream()
                    .filter(uor -> uor.getOrg().getId().equals(orgId))
                    .collect(Collectors.toSet());
        } else {
            // If orgId not present (login scenario), get ALL roles for this user
            roles = user.getUserOrgRoles();
        }

        Set<GrantedAuthority> authorities = roles.stream()
                .map(uor -> new SimpleGrantedAuthority(uor.getRole().name()))
                .collect(Collectors.toSet());

        return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(), authorities);
    }

    // All other service methods: orgId is REQUIRED
    public List<User> getAllUsers() {
        Long orgId = getCurrentOrgId();
        final Long finalOrgId = orgId;
        List<User> users = userRepository.findAll().stream()
                .filter(user -> user.getUserOrgRoles().stream()
                        .anyMatch(uor -> uor.getOrg().getId().equals(finalOrgId)))
                .collect(Collectors.toList());
        users.forEach(u -> u.setPassword("<Hidden>"));
        return users;
    }

    public Optional<User> getUserByEmail(String email) {
        Long orgId = getCurrentOrgId();
        final Long finalOrgId = orgId;
        Optional<User> user = userRepository.findByEmail(email)
                .filter(u -> u.getUserOrgRoles().stream()
                        .anyMatch(uor -> uor.getOrg().getId().equals(finalOrgId)));
        user.ifPresent(value -> value.setPassword("<Hidden>"));
        return user;
    }

    @Transactional
    public User assignUserToOrg(User user, Long orgId, RoleName roleName) {
        if (orgId == null) orgId = getCurrentOrgId();

        final Long finalOrgId = orgId;
        final RoleName finalRoleName = roleName;

        Org org = orgRepository.findById(finalOrgId)
                .orElseThrow(() -> new RuntimeException("Org not found"));

        User persistedUser = userRepository.findByEmail(user.getEmail()).orElse(userRepository.save(user));

        boolean alreadyAssigned = userOrgRoleRepository
                .findByUserId(persistedUser.getId())
                .stream()
                .anyMatch(uor -> uor.getOrg().getId().equals(finalOrgId) && uor.getRole() == finalRoleName);

        if (alreadyAssigned) {
            throw new RuntimeException("User is already assigned to this org with the same role.");
        }

        UserOrgRole uor = UserOrgRole.builder()
                .user(persistedUser)
                .org(org)
                .role(finalRoleName)
                .build();

        userOrgRoleRepository.save(uor);
        persistedUser.getUserOrgRoles().add(uor);
        org.getUserOrgRoles().add(uor);

        userRepository.save(persistedUser);
        orgRepository.save(org);

        persistedUser.setPassword("<Hidden>");
        return persistedUser;
    }

    public User updateUserByEmail(String email, User userDetails) {
        Long orgId = getCurrentOrgId();
        final Long finalOrgId = orgId;
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean hasOrg = user.getUserOrgRoles().stream()
                .anyMatch(uor -> uor.getOrg().getId().equals(finalOrgId));
        if (!hasOrg) throw new RuntimeException("User not in this org");

        user.setFirstName(userDetails.getFirstName());
        user.setLastName(userDetails.getLastName());
        user.setMiddleName(userDetails.getMiddleName());
        user.setDateOfBirth(userDetails.getDateOfBirth());
        user.setEmail(userDetails.getEmail());
        user.setPhoneNumber(userDetails.getPhoneNumber());
        user.setProfileImage(userDetails.getProfileImage());
        user.setStreet(userDetails.getStreet());
        user.setStreet2(userDetails.getStreet2());
        user.setCity(userDetails.getCity());
        user.setState(userDetails.getState());
        user.setPostalCode(userDetails.getPostalCode());
        user.setCountry(userDetails.getCountry());
        user.setSecurityQuestion(userDetails.getSecurityQuestion());
        user.setSecurityAnswer(userDetails.getSecurityAnswer());

        User updated = userRepository.save(user);
        updated.setPassword("<Hidden>");
        return updated;
    }

    @Transactional
    public void deleteUserByEmail(String email) {
        Long orgId = getCurrentOrgId();
        final Long finalOrgId = orgId;
        Optional<User> userOpt = userRepository.findByEmail(email)
                .filter(u -> u.getUserOrgRoles().stream()
                        .anyMatch(uor -> uor.getOrg().getId().equals(finalOrgId)));
        userOpt.ifPresent(userRepository::delete);
    }

    @Transactional
    public void removeUserFromOrg(String email, Long orgId, RoleName roleName) {
        if (orgId == null) orgId = getCurrentOrgId();

        final Long finalOrgId = orgId;
        final RoleName finalRoleName = roleName;

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Org org = orgRepository.findById(finalOrgId)
                .orElseThrow(() -> new RuntimeException("Org not found"));

        List<UserOrgRole> assignments = userOrgRoleRepository.findByUserId(user.getId()).stream()
                .filter(uor -> uor.getOrg().getId().equals(finalOrgId) && uor.getRole() == finalRoleName)
                .collect(Collectors.toList());
        assignments.forEach(uor -> userOrgRoleRepository.deleteById(uor.getId()));

        user.getUserOrgRoles().removeIf(
                uor -> uor.getOrg().getId().equals(finalOrgId) && uor.getRole() == finalRoleName);
        org.getUserOrgRoles().removeIf(
                uor -> uor.getUser().getId().equals(user.getId()) && uor.getRole() == finalRoleName);

        userRepository.save(user);
        orgRepository.save(org);
    }

    public List<UserOrgRole> getOrgRolesForUser(String email) {
        Long orgId = getCurrentOrgId();
        final Long finalOrgId = orgId;
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return user.getUserOrgRoles().stream()
                .filter(uor -> uor.getOrg().getId().equals(finalOrgId))
                .collect(Collectors.toList());
    }

    private Long getCurrentOrgId() {
        RequestContext ctx = RequestContext.get();
        if (ctx == null || ctx.getOrgId() == null) {
            throw new IllegalStateException("No orgId set in RequestContext");
        }
        return ctx.getOrgId();
    }
}
