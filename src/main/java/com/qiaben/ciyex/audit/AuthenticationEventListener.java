package com.qiaben.ciyex.audit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class AuthenticationEventListener {

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private HttpServletRequest request;

    @EventListener
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Authentication authentication = event.getAuthentication();
        String username = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        auditLogService.logLogin(username, role);
    }

    @EventListener
    public void handleLogoutSuccess(LogoutSuccessEvent event) {
        Authentication authentication = event.getAuthentication();
        String username = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        auditLogService.logLogout(username, role);
    }
}