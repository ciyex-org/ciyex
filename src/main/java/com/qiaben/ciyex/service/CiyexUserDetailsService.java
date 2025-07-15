package com.qiaben.ciyex.service;

import com.qiaben.ciyex.entity.*;
import com.qiaben.ciyex.repository.OrgRepository;
import com.qiaben.ciyex.repository.UserRepository;
import com.qiaben.ciyex.repository.FacilityRepository;
import com.qiaben.ciyex.repository.UserFacilityRoleRepository;
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
    private FacilityRepository facilityRepository;
    @Autowired
    private UserFacilityRoleRepository userFacilityRoleRepository;

    // Used by Spring Security, logs in by email alone (regardless of org)
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // Defensive copy to avoid ConcurrentModificationException
        Set<UserFacilityRole> safeRoles = new HashSet<>(user.getUserFacilityRoles());
        Set<GrantedAuthority> authorities = safeRoles.stream()
                .map(ufr -> new SimpleGrantedAuthority(ufr.getRole().name()))
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

    // Assign user to facility with a specific role (creates new user if needed)
    @Transactional
    public User assignUserToFacility(User user, Long facilityId, RoleName roleName) {
        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new RuntimeException("Facility not found"));

        User persistedUser = userRepository.findByEmail(user.getEmail()).orElse(userRepository.save(user));

        // Check if the user already has the role in the facility
        boolean alreadyAssigned = userFacilityRoleRepository
                .findByUser_Id(persistedUser.getId())
                .stream()
                .anyMatch(ufr -> ufr.getFacility().getId().equals(facilityId) && ufr.getRole() == roleName);

        if (alreadyAssigned) {
            throw new RuntimeException("User is already assigned to this facility with the same role.");
        }

        UserFacilityRole ufr = UserFacilityRole.builder()
                .user(persistedUser)
                .facility(facility)
                .role(roleName)
                .build();

        // Save to DB and ensure relationships are up to date
        userFacilityRoleRepository.save(ufr);

        // If you maintain bidirectional mapping, update local sets
        persistedUser.getUserFacilityRoles().add(ufr);
        facility.getUserFacilityRoles().add(ufr);

        userRepository.save(persistedUser);
        facilityRepository.save(facility);

        persistedUser.setPassword("<Hidden>");
        return persistedUser;
    }

    // Update user info
    public User updateUserByEmail(String email, User userDetails) {
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

        User updated = userRepository.save(user);
        updated.setPassword("<Hidden>");
        return updated;
    }

    // Remove user completely (careful: this will delete user and all their facility-role assignments!)
    @Transactional
    public void deleteUserByEmail(String email) {
        userRepository.deleteByEmail(email);
    }

    // Remove user's role assignment from a facility (using triple-key)
    @Transactional
    public void removeUserFromFacility(String email, Long facilityId, RoleName roleName) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new RuntimeException("Facility not found"));

        // Find the assignment
        List<UserFacilityRole> assignments = userFacilityRoleRepository.findByUser_Id(user.getId()).stream()
                .filter(ufr -> ufr.getFacility().getId().equals(facilityId) && ufr.getRole() == roleName)
                .collect(Collectors.toList());
        assignments.forEach(ufr -> userFacilityRoleRepository.deleteById(ufr.getId()));

        // Remove from local sets
        user.getUserFacilityRoles().removeIf(
                ufr -> ufr.getFacility().getId().equals(facilityId) && ufr.getRole() == roleName);
        facility.getUserFacilityRoles().removeIf(
                ufr -> ufr.getUser().getId().equals(user.getId()) && ufr.getRole() == roleName);

        userRepository.save(user);
        facilityRepository.save(facility);
    }

    // Get all orgs, facilities, and roles for a user (to build login response)
    public List<UserFacilityRole> getFacilityRolesForUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        // Defensive copy
        return new ArrayList<>(user.getUserFacilityRoles());
    }

}
