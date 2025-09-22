package com.qiaben.ciyex.service.portal.service;

import com.qiaben.ciyex.entity.portal.entity.PortalUser;
import com.qiaben.ciyex.repository.portal.repository.PortalUserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class PortalUserDetailsService implements UserDetailsService {

    @Autowired
    private PortalUserRepository portalUserRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        PortalUser user = portalUserRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Patient not found with email: " + email));

        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_PATIENT");

        // ✅ Return custom PortalUserDetails (includes patient id)
        return new PortalUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPassword(),   // must be encoded with PasswordEncoder when saving
                Collections.singleton(authority)
        );
    }
}
