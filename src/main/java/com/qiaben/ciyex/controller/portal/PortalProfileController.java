package com.qiaben.ciyex.controller.portal;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qiaben.ciyex.dto.portal.PortalUserDto;
import com.qiaben.ciyex.entity.portal.PortalUser;
import com.qiaben.ciyex.repository.portal.PortalUserRepository;

@RestController
@RequestMapping("/api/portal/profile")
@RequiredArgsConstructor
public class PortalProfileController {

    private final PortalUserRepository userRepository;

    /**
     * GET /api/portal/profile - returns current portal user's profile
     */
    @GetMapping
    public ResponseEntity<PortalUserDto> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        
        String email = authentication.getName();
        PortalUser user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();
        PortalUserDto dto = PortalUserDto.fromEntity(user);
        return ResponseEntity.ok(dto);
    }

    /**
     * PUT /api/portal/profile - update current portal user's profile (partial/full)
     */
    @PutMapping
    public ResponseEntity<PortalUserDto> update(Authentication authentication, @RequestBody PortalUserDto updated) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        
        String email = authentication.getName();
        PortalUser user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        // minimal safe update: allow certain fields to be updated
        if (updated.getFirstName() != null) user.setFirstName(updated.getFirstName());
        if (updated.getLastName() != null) user.setLastName(updated.getLastName());
        if (updated.getPhoneNumber() != null) user.setPhoneNumber(updated.getPhoneNumber());

        PortalUser saved = userRepository.save(user);
        PortalUserDto dto = PortalUserDto.fromEntity(saved);
        return ResponseEntity.ok(dto);
    }
}