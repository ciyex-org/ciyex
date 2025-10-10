package com.qiaben.ciyex.security;

import com.qiaben.ciyex.entity.User;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SuperAdminConfig {

    @Value("${security.superadmin.enabled:true}")
    private boolean enabled;

    @Value("${security.superadmin.emails:}")
    private String emailsCsv;

    @Value("${security.superadmin.user-ids:}")
    private String userIdsCsv;

    private Set<String> emailSet;
    private Set<Long> userIdSet;

    @PostConstruct
    void init() {
        emailSet = Arrays.stream((emailsCsv == null ? "" : emailsCsv).split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(String::toLowerCase).collect(Collectors.toCollection(HashSet::new));

        userIdSet = Arrays.stream((userIdsCsv == null ? "" : userIdsCsv).split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(Long::valueOf).collect(Collectors.toCollection(HashSet::new));
    }

    public boolean isSuperAdmin(User user) {
        if (!enabled || user == null) return false;
        if (userIdSet.contains(user.getId())) return true;
        String email = user.getEmail() == null ? "" : user.getEmail().toLowerCase();
        return emailSet.contains(email);
    }
}
