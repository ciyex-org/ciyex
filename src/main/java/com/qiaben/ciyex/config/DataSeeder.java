package com.qiaben.ciyex.config;

import com.qiaben.ciyex.model.User;
import com.qiaben.ciyex.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            userRepository.save(User.builder()
                    .fullName("Admin User")
                    .email("admin@demo.com")
                    .password(passwordEncoder.encode("admin123"))
                    .role(User.Role.ADMIN)
                    .build());

            userRepository.save(User.builder()
                    .fullName("Doctor John")
                    .email("doctor@demo.com")
                    .password(passwordEncoder.encode("admin123"))
                    .role(User.Role.DOCTOR)
                    .build());

            userRepository.save(User.builder()
                    .fullName("Patient Smith")
                    .email("patient@demo.com")
                    .password(passwordEncoder.encode("admin123"))
                    .role(User.Role.PATIENT)
                    .build());

            userRepository.save(User.builder()
                    .fullName("Nurse Kelly")
                    .email("nurse@demo.com")
                    .password(passwordEncoder.encode("admin123"))
                    .role(User.Role.NURSE)
                    .build());

            userRepository.save(User.builder()
                    .fullName("Receptionist Jane")
                    .email("receptionist@demo.com")
                    .password(passwordEncoder.encode("admin123"))
                    .role(User.Role.RECEPTIONIST)
                    .build());
        }
    }
}
