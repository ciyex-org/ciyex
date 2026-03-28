package org.ciyex.ehr.audit.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.ciyex.ehr.audit.dto.AuditLogDto;
import org.ciyex.ehr.audit.service.AuditLogService;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AOP aspect that automatically intercepts controller write operations (POST, PUT, DELETE)
 * and creates audit log entries with user information extracted from the JWT security context.
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogService auditLogService;

    // ── Pointcuts for write operations only (skip GET to avoid noise) ────────

    @Pointcut("@annotation(org.springframework.web.bind.annotation.PostMapping)")
    public void postMappingMethods() {}

    @Pointcut("@annotation(org.springframework.web.bind.annotation.PutMapping)")
    public void putMappingMethods() {}

    @Pointcut("@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public void deleteMappingMethods() {}

    @Pointcut("postMappingMethods() || putMappingMethods() || deleteMappingMethods()")
    public void writeMappingMethods() {}

    // Only intercept controllers in the org.ciyex.ehr package
    @Pointcut("within(org.ciyex.ehr..*)")
    public void withinEhrPackage() {}

    // Exclude the AuditLogController itself to prevent recursive logging
    @Pointcut("!within(org.ciyex.ehr.audit..*)")
    public void notAuditController() {}

    // ── Around advice ────────────────────────────────────────────────────────

    @Around("writeMappingMethods() && withinEhrPackage() && notAuditController()")
    public Object auditWriteOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        // Execute the actual method first
        Object result = joinPoint.proceed();

        // Audit logging in a try-catch so failures never break the main request
        try {
            logAuditEntry(joinPoint);
        } catch (Exception e) {
            log.warn("Audit logging failed for {}.{}: {}",
                    joinPoint.getSignature().getDeclaringType().getSimpleName(),
                    joinPoint.getSignature().getName(),
                    e.getMessage());
        }

        return result;
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private void logAuditEntry(ProceedingJoinPoint joinPoint) {
        String action = determineAction(joinPoint);
        String resourceType = determineResourceType(joinPoint);
        String ipAddress = resolveIpAddress();
        String orgAlias = resolveOrgAlias();

        // Extract user info from JWT
        String userName = null;
        String userRole = null;
        String userId = null;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();

            // preferred_username is the standard Keycloak claim for the human-readable username
            userName = jwt.getClaimAsString("preferred_username");
            if (userName == null || userName.isBlank()) {
                userName = jwt.getClaimAsString("name");
            }

            userId = jwt.getSubject(); // Keycloak sub = user UUID

            // Resolve role: prefer RequestContext (already resolved by RequestContextInterceptor),
            // fall back to extracting from JWT authorities
            userRole = resolveUserRole(jwtAuth);
        }

        // Build resource name from the method name for additional context
        String resourceName = buildResourceName(joinPoint);

        // Build details from method arguments (capture key info without sensitive data)
        String details = buildDetails(joinPoint, action);

        AuditLogDto dto = AuditLogDto.builder()
                .action(action)
                .resourceType(resourceType)
                .resourceName(resourceName)
                .userId(userId)
                .userName(userName)
                .userRole(userRole)
                .ipAddress(ipAddress)
                .details(details)
                .build();

        auditLogService.log(dto);

        log.debug("Audit logged: action={}, resourceType={}, user={}, role={}, org={}",
                action, resourceType, userName, userRole, orgAlias);
    }

    /**
     * Determine the CRUD action from which mapping annotation is present on the intercepted method.
     */
    private String determineAction(ProceedingJoinPoint joinPoint) {
        try {
            var method = getMethod(joinPoint);
            if (method.isAnnotationPresent(org.springframework.web.bind.annotation.PostMapping.class)) {
                return "CREATE";
            }
            if (method.isAnnotationPresent(org.springframework.web.bind.annotation.PutMapping.class)) {
                return "UPDATE";
            }
            if (method.isAnnotationPresent(org.springframework.web.bind.annotation.DeleteMapping.class)) {
                return "DELETE";
            }
        } catch (NoSuchMethodException e) {
            log.debug("Could not resolve method for action determination: {}", e.getMessage());
        }
        return "UNKNOWN";
    }

    /**
     * Derive the resource type from the controller class name or URL path.
     * For GenericFhirResourceController and FhirFacadeController, extract the
     * actual resource type from the request URL path (e.g., /api/fhir-resource/immunizations/... -> "Immunizations").
     * For other controllers: PatientBillingController -> "PatientBilling"
     */
    private String determineResourceType(ProceedingJoinPoint joinPoint) {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String base = className
                .replace("Controller", "")
                .replace("RestController", "");

        // For generic controllers, try to extract meaningful resource type from URL
        if ("GenericFhirResource".equals(base) || "FhirFacade".equals(base)) {
            try {
                ServletRequestAttributes attrs =
                        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    String uri = attrs.getRequest().getRequestURI();
                    // Match /api/fhir-resource/{tabKey}/... or /api/{tabKey} patterns
                    var matcher = java.util.regex.Pattern.compile("/api/(?:fhir-resource/)?([a-zA-Z][a-zA-Z0-9_-]+)")
                            .matcher(uri);
                    if (matcher.find()) {
                        String tabKey = matcher.group(1);
                        // Capitalize first letter for display
                        return Character.toUpperCase(tabKey.charAt(0)) + tabKey.substring(1);
                    }
                }
            } catch (Exception e) {
                log.debug("Could not extract resource type from URL: {}", e.getMessage());
            }
        }

        return base;
    }

    /**
     * Build a human-readable resource name from the method being called.
     * e.g. "createPatient" -> "Create Patient", "updateEncounter" -> "Update Encounter"
     */
    private String buildResourceName(ProceedingJoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        // Insert space before each uppercase letter and capitalize first letter
        String spaced = methodName.replaceAll("([a-z])([A-Z])", "$1 $2");
        if (!spaced.isEmpty()) {
            spaced = Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
        }
        return spaced;
    }

    /**
     * Resolve the user's primary role. Prefer the already-resolved role from RequestContext
     * (set by RequestContextInterceptor), fall back to extracting ROLE_ authorities from JWT.
     */
    private String resolveUserRole(JwtAuthenticationToken jwtAuth) {
        // First try RequestContext which already resolved the primary role
        try {
            RequestContext ctx = RequestContext.get();
            if (ctx != null && ctx.getUserRole() != null && !ctx.getUserRole().isBlank()) {
                return ctx.getUserRole();
            }
        } catch (Exception e) {
            // RequestContext may not be available
        }

        // Fall back to extracting from JWT authorities
        List<String> roles = jwtAuth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .filter(r -> !r.equals("CIYEX_SUPER_ADMIN"))
                .collect(Collectors.toList());

        return roles.isEmpty() ? null : roles.getFirst();
    }

    /**
     * Resolve the client IP address from the current HTTP request.
     */
    private String resolveIpAddress() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                // Check X-Forwarded-For header first (common behind load balancers/proxies)
                String forwarded = request.getHeader("X-Forwarded-For");
                if (forwarded != null && !forwarded.isBlank()) {
                    // X-Forwarded-For can contain multiple IPs; first one is the client
                    return forwarded.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("Could not resolve IP address: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Resolve org alias from RequestContext (set by RequestContextInterceptor).
     */
    private String resolveOrgAlias() {
        try {
            RequestContext ctx = RequestContext.get();
            if (ctx != null) {
                return ctx.getOrgName();
            }
        } catch (Exception e) {
            log.debug("Could not resolve org alias: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Build a JSON details string from method arguments for audit logging.
     * Captures method name, request URI, and path variable IDs — avoids logging full request bodies.
     */
    private String buildDetails(ProceedingJoinPoint joinPoint, String action) {
        try {
            var details = new java.util.LinkedHashMap<String, Object>();
            details.put("method", joinPoint.getSignature().getName());

            // Include request URI for context
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String uri = attrs.getRequest().getRequestURI();
                String httpMethod = attrs.getRequest().getMethod();
                details.put("endpoint", httpMethod + " " + uri);

                // Extract resource IDs from path variables
                var signature = (org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature();
                String[] paramNames = signature.getParameterNames();
                Object[] args = joinPoint.getArgs();
                if (paramNames != null && args != null) {
                    for (int i = 0; i < paramNames.length; i++) {
                        String name = paramNames[i];
                        Object arg = args[i];
                        // Capture ID-like path variables and simple primitives
                        if (arg != null && (name.toLowerCase().contains("id") || name.equals("status"))
                                && (arg instanceof String || arg instanceof Number)) {
                            details.put(name, arg);
                        }
                    }
                }
            }

            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(details);
        } catch (Exception e) {
            log.debug("Could not build audit details: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Reflectively get the actual method from the join point to check annotations.
     */
    private java.lang.reflect.Method getMethod(ProceedingJoinPoint joinPoint) throws NoSuchMethodException {
        var signature = (org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature();
        return signature.getMethod();
    }
}
