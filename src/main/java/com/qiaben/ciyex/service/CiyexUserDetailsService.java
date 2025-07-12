package com.qiaben.ciyex.service;

import com.qiaben.ciyex.entity.Org;
import com.qiaben.ciyex.entity.User;
import com.qiaben.ciyex.repository.OrgRepository;
import com.qiaben.ciyex.repository.UserRepository;
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

    // Used by Spring Security, logs in by email alone (regardless of org)
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toSet());

        return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(), authorities);
    }

    public List<User> getAllUsers() {
        List<User> users = userRepository.findAll();
        users.forEach(u -> u.setPassword("<Hidden>"));
        return users;
    }

    public Optional<User> getUserByEmail(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        user.ifPresent(value -> value.setPassword("<Hidden>"));
        return user;
    }

    // For multi-org, must check if user already exists for this org
    public User createUser(User user, Long orgId) {
        Org org = orgRepository.findById(orgId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        // Try to find an existing user with this email
        Optional<User> existingUserOpt = userRepository.findByEmail(user.getEmail());
        User savedUser;

        if (existingUserOpt.isPresent()) {
            // Attach existing user to new org if not already attached
            User existingUser = existingUserOpt.get();
            if (existingUser.getOrgs().stream().anyMatch(o -> Objects.equals(o.getId(), orgId))) {
                throw new RuntimeException("User already exists for this organization");
            }
            existingUser.getOrgs().add(org);
            savedUser = userRepository.save(existingUser);
        } else {
            // Create new user and assign org
            user.getOrgs().add(org);
            savedUser = userRepository.save(user);
        }

        savedUser.setPassword("<Hidden>");
        return savedUser;
    }

    // Update user info and org memberships (optionally add to org)
    public User updateUserByEmail(String email, User userDetails, Long orgId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setFullName(userDetails.getFullName());
        user.setDateOfBirth(userDetails.getDateOfBirth());
        user.setEmail(userDetails.getEmail());
        user.setPhoneNumber(userDetails.getPhoneNumber());
        user.setProfileImage(userDetails.getProfileImage());
        user.setStreet(userDetails.getStreet());
        user.setCity(userDetails.getCity());
        user.setState(userDetails.getState());
        user.setPostalCode(userDetails.getPostalCode());
        user.setCountry(userDetails.getCountry());
        user.setSecurityQuestion(userDetails.getSecurityQuestion());
        user.setSecurityAnswer(userDetails.getSecurityAnswer());

        // Optionally, attach to another org if orgId provided
        if (orgId != null) {
            Org org = orgRepository.findById(orgId)
                    .orElseThrow(() -> new RuntimeException("Organization not found"));
            if (user.getOrgs().stream().noneMatch(o -> Objects.equals(o.getId(), orgId))) {
                user.getOrgs().add(org);
            }
        }

        User updated = userRepository.save(user);
        updated.setPassword("<Hidden>");
        return updated;
    }

    // Remove user completely (careful: this will delete user from ALL orgs!)
    @Transactional
    public void deleteUserByEmail(String email) {
        userRepository.deleteByEmail(email);
    }

    // Optional: Remove user from a single org
    @Transactional
    public void removeUserFromOrg(String email, Long orgId) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.getOrgs().removeIf(org -> Objects.equals(org.getId(), orgId));
            userRepository.save(user);
        }
    }

    public Optional<User> getUserByEmailAndOrg(String email, Long orgId) {
        return userRepository.findByEmailAndOrgs_Id(email, orgId);
    }

    public User createUserForOrg(User user, Long orgId) {
        // Check if org exists
        Org org = orgRepository.findById(orgId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        // Check if user with this email exists; if not, create, else just add org to their orgs
        Optional<User> existingUser = userRepository.findByEmail(user.getEmail());
        if (existingUser.isPresent()) {
            User u = existingUser.get();
            u.getOrgs().add(org);
            return userRepository.save(u);
        } else {
            user.getOrgs().add(org);
            User savedUser = userRepository.save(user);
            savedUser.setPassword("<Hidden>");
            return savedUser;
        }
    }

}
