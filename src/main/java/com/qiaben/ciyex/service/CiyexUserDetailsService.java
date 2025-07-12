package com.qiaben.ciyex.service;

import com.qiaben.ciyex.entity.User;
import com.qiaben.ciyex.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CiyexUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

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
        users = users.stream().map(user -> {
            user.setPassword("<Hidden>");
            return user;
        }).collect(Collectors.toList());
        return users;
    }

    public Optional<User> getUserByEmail(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        user.ifPresent(value -> value.setPassword("<Hidden>"));
        return user;
    }

    public User createUser(User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("User already exists");
        }
        User savedUser = userRepository.save(user);
        savedUser.setPassword("<Hidden>");
        return savedUser;
    }

    public User updateUserByEmail(String email, User userDetails) {
        return userRepository.findByEmail(email)
                .map(user -> {
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
                    return userRepository.save(user);
                }).orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public void deleteUserByEmail(String email) {
        userRepository.deleteByEmail(email);
    }
}
