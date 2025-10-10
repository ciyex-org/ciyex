package com.qiaben.ciyex.controller.portal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qiaben.ciyex.dto.portal.PortalUserDto;
import com.qiaben.ciyex.entity.portal.PortalUser;
import com.qiaben.ciyex.repository.portal.PortalUserRepository;
import com.qiaben.ciyex.util.JwtTokenUtil;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/portal/profile")
public class PortalProfileController {

    private final PortalUserRepository userRepository;
    private final JwtTokenUtil jwtUtil;

    public PortalProfileController(PortalUserRepository userRepository, JwtTokenUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    /**
     * GET /api/portal/profile - returns current portal user's profile
     */
    @GetMapping
    public ResponseEntity<PortalUserDto> me(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) return ResponseEntity.status(401).build();
        Long userId = jwtUtil.getUserIdFromToken(token);
        PortalUser user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();
        PortalUserDto dto = PortalUserDto.fromEntity(user);
        return ResponseEntity.ok(dto);
    }

    /**
     * PUT /api/portal/profile - update current portal user's profile (partial/full)
     */
    @PutMapping
    public ResponseEntity<PortalUserDto> update(HttpServletRequest request, @RequestBody PortalUserDto updated) {
        String token = resolveToken(request);
        if (token == null) return ResponseEntity.status(401).build();
        Long userId = jwtUtil.getUserIdFromToken(token);
        PortalUser user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        // minimal safe update: allow certain fields to be updated
        if (updated.getFirstName() != null) user.setFirstName(updated.getFirstName());
        if (updated.getLastName() != null) user.setLastName(updated.getLastName());
        if (updated.getPhoneNumber() != null) user.setPhoneNumber(updated.getPhoneNumber());

        PortalUser saved = userRepository.save(user);
        PortalUserDto dto = PortalUserDto.fromEntity(saved);
        return ResponseEntity.ok(dto);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}