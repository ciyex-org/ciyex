package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.LoginResponse;
import com.qiaben.ciyex.dto.LoginRequest;
import com.qiaben.ciyex.model.User;
import com.qiaben.ciyex.repository.UserRepository;
import com.qiaben.ciyex.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authManager;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        Authentication authentication = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Optional: check expectedRole match
        if (request.getExpectedRole() != null && !user.getRole().name().equalsIgnoreCase(request.getExpectedRole())) {
            throw new BadCredentialsException("Access denied for role: " + request.getExpectedRole());
        }

        String token = jwtService.generateToken(authentication, user.getRole().name());

        return new LoginResponse(token, user.getRole().name());
    }
}
