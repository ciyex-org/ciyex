package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ChangePasswordRequest;
import com.qiaben.ciyex.dto.UpdateAddressRequest;
import com.qiaben.ciyex.dto.UpdateUserProfileRequest;
import com.qiaben.ciyex.entity.User;
import com.qiaben.ciyex.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    public void updateUserProfileByEmail(String email, UpdateUserProfileRequest request) throws Exception {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new Exception("User not found with email: " + email));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setDateOfBirth(request.getDateOfBirth());
        userRepository.save(user);
    }

    // src/main/java/com/qiaben/ciyex/service/UserService.java
    public void updateUserAddress(String email, UpdateAddressRequest request) throws Exception {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new Exception("User not found with email: " + email));

        user.setStreet(request.getStreet());
        user.setStreet2(request.getStreet2());
        user.setCity(request.getCity());
        user.setState(request.getState());
        user.setPostalCode(request.getPostalCode());
        user.setCountry(request.getCountry());

        userRepository.save(user);
    }
    public void changeUserPassword(ChangePasswordRequest request) throws Exception {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new Exception("User not found with email: " + request.getEmail()));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new Exception("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && 
            !authentication.getPrincipal().equals("anonymousUser")) {
            String email = authentication.getName();
            return userRepository.findByEmail(email).orElse(null);
        }
        return null;
    }

}
