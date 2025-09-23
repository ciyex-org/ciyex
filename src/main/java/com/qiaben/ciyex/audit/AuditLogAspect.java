package com.qiaben.ciyex.audit;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletRequest;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * AOP Aspect for automatic audit logging of EHR operations
 * 
 * This aspect automatically captures auditable events as required by
 * ONC § 170.315(d)(2) for comprehensive system activity monitoring.
 * 
 * It intercepts:
 * - All controller methods (API access)
 * - Service layer CRUD operations  
 * - Security-sensitive operations
 * - Failed operations and exceptions
 */
@Aspect
@Component
public class AuditLogAspect {

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private HttpServletRequest request;

    /**
     * Log all REST controller method calls
     * Captures all API access as required by ONC § 170.315(d)(2)
     */
    @AfterReturning(pointcut = "within(@org.springframework.web.bind.annotation.RestController *)", returning = "result")
    public void logControllerMethod(JoinPoint joinPoint, Object result) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return;
            }

            String username = authentication.getName();
            String role = authentication.getAuthorities().iterator().next().getAuthority();
            String ipAddress = getClientIpAddress();
            String endpoint = request.getRequestURI();

            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String methodName = signature.getMethod().getName();
            String className = joinPoint.getTarget().getClass().getSimpleName();

            String actionType = determineActionType(methodName, className);
            String entityType = determineEntityType(className);
            String entityId = extractEntityId(joinPoint, result);
            String description = buildDescription(className, methodName, joinPoint.getArgs());

            auditLogService.logEvent(actionType, entityType, entityId, description, null,
                    username, role, ipAddress, endpoint);

        } catch (Exception e) {
            System.err.println("Audit logging failed: " + e.getMessage());
        }
    }

    /**
     * Log service layer CRUD operations
     * Captures business logic operations for comprehensive auditing
     */
    @AfterReturning(
            pointcut = "within(@org.springframework.stereotype.Service *) && " +
                    "(execution(* *.create*(..)) || execution(* *.update*(..)) || " +
                    "execution(* *.delete*(..)) || execution(* *.save*(..)))",
            returning = "result"
    )
    public void logServiceMethod(JoinPoint joinPoint, Object result) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return;
            }

            String username = authentication.getName();
            String role = authentication.getAuthorities().iterator().next().getAuthority();
            String ipAddress = getClientIpAddress();
            String endpoint = request.getRequestURI();

            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String methodName = signature.getMethod().getName();
            String className = joinPoint.getTarget().getClass().getSimpleName();

            String actionType = determineActionType(methodName, className);
            String entityType = determineEntityType(className);
            String entityId = extractEntityId(joinPoint, result);
            String description = buildDescription(className, methodName, joinPoint.getArgs());

            auditLogService.logEvent(actionType, entityType, entityId, description, null,
                    username, role, ipAddress, endpoint);

        } catch (Exception e) {
            System.err.println("Audit logging failed: " + e.getMessage());
        }
    }

    /**
     * Log failed operations and exceptions
     * Critical for security monitoring per ONC requirements
     */
    @AfterThrowing(pointcut = "within(@org.springframework.web.bind.annotation.RestController *)", throwing = "exception")
    public void logControllerException(JoinPoint joinPoint, Throwable exception) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication != null ? authentication.getName() : "SYSTEM";
            String role = authentication != null && authentication.isAuthenticated() ? 
                    authentication.getAuthorities().iterator().next().getAuthority() : "UNAUTHENTICATED";
            
            String ipAddress = getClientIpAddress();
            String endpoint = request.getRequestURI();

            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String methodName = signature.getMethod().getName();
            String className = joinPoint.getTarget().getClass().getSimpleName();

            String actionType = determineActionType(methodName, className);
            String entityType = determineEntityType(className);
            String description = "FAILED: " + buildDescription(className, methodName, joinPoint.getArgs());

            // Log the failure through the audit service
            auditLogService.logFailedEvent(actionType, entityType, null, description, 
                    exception.getMessage(), username, role, ipAddress, endpoint, 500);

        } catch (Exception e) {
            System.err.println("Exception audit logging failed: " + e.getMessage());
        }
    }

    /**
     * Log authentication events specifically
     * Required by ONC § 170.315(d)(1) for authentication auditing
     */
    @AfterReturning(pointcut = "execution(* com.qiaben.ciyex..AuthenticationController.*(..))", returning = "result")
    public void logAuthenticationEvent(JoinPoint joinPoint, Object result) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String methodName = signature.getMethod().getName();
            
            if ("login".equals(methodName)) {
                // Login success will be handled by the service
                return;
            } else if ("logout".equals(methodName)) {
                // Logout will be handled by the service
                return;
            }
        } catch (Exception e) {
            System.err.println("Authentication audit logging failed: " + e.getMessage());
        }
    }

    /**
     * Log patient data access specifically
     * Critical for ONC § 170.315(d)(2) patient data access auditing
     */
    @AfterReturning(pointcut = "execution(* com.qiaben.ciyex..PatientController.*(..))", returning = "result")
    public void logPatientDataAccess(JoinPoint joinPoint, Object result) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return;
            }

            String username = authentication.getName();
            String role = authentication.getAuthorities().iterator().next().getAuthority();

            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String methodName = signature.getMethod().getName();
            
            // Extract patient ID from method arguments
            Object[] args = joinPoint.getArgs();
            Long patientId = extractPatientIdFromArgs(args);
            
            if (patientId != null) {
                String actionType = determineActionType(methodName, "Patient");
                String description = "Patient data " + actionType.toLowerCase() + " operation";
                
                auditLogService.logPatientEvent(actionType, patientId, "PATIENT", patientId.toString(),
                        description, null);
            }

        } catch (Exception e) {
            System.err.println("Patient data audit logging failed: " + e.getMessage());
        }
    }

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    private String getClientIpAddress() {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    private String determineActionType(String methodName, String className) {
        String lowerMethodName = methodName.toLowerCase();
        
        // Specific method name patterns
        if (lowerMethodName.startsWith("create") || lowerMethodName.startsWith("add") || 
            lowerMethodName.startsWith("save") || lowerMethodName.startsWith("insert")) {
            return "CREATE";
        } else if (lowerMethodName.startsWith("update") || lowerMethodName.startsWith("modify") || 
                  lowerMethodName.startsWith("edit") || lowerMethodName.startsWith("change")) {
            return "UPDATE";
        } else if (lowerMethodName.startsWith("delete") || lowerMethodName.startsWith("remove")) {
            return "DELETE";
        } else if (lowerMethodName.startsWith("get") || lowerMethodName.startsWith("find") || 
                  lowerMethodName.startsWith("read") || lowerMethodName.startsWith("view") ||
                  lowerMethodName.startsWith("list") || lowerMethodName.startsWith("search")) {
            return "VIEW";
        } else if (lowerMethodName.contains("login")) {
            return "LOGIN";
        } else if (lowerMethodName.contains("logout")) {
            return "LOGOUT";
        } else if (lowerMethodName.contains("export")) {
            return "EXPORT";
        } else if (lowerMethodName.contains("import")) {
            return "IMPORT";
        } else if (lowerMethodName.contains("download")) {
            return "DOWNLOAD";
        } else if (lowerMethodName.contains("upload")) {
            return "UPLOAD";
        }
        
        // HTTP method based determination
        String httpMethod = request.getMethod();
        switch (httpMethod) {
            case "GET":
                return "VIEW";
            case "POST":
                return "CREATE";
            case "PUT":
            case "PATCH":
                return "UPDATE";
            case "DELETE":
                return "DELETE";
            default:
                return "ACCESS";
        }
    }

    private String determineEntityType(String className) {
        // Remove "Controller" or "Service" suffix
        String entityName = className.replace("Controller", "").replace("Service", "");
        
        // Convert camelCase to UPPER_CASE
        return entityName.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
    }

    private String extractEntityId(JoinPoint joinPoint, Object result) {
        // Try to extract ID from result object
        if (result != null) {
            try {
                // Check if result has an 'id' field
                Field idField = result.getClass().getDeclaredField("id");
                idField.setAccessible(true);
                Object id = idField.get(result);
                return id != null ? id.toString() : null;
            } catch (Exception e) {
                // Ignore if no id field found
            }
            
            // Check if result has getId() method
            try {
                Method getIdMethod = result.getClass().getMethod("getId");
                Object id = getIdMethod.invoke(result);
                return id != null ? id.toString() : null;
            } catch (Exception e) {
                // Ignore if no getId method found
            }
        }
        
        // Try to extract ID from method arguments
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof Long || arg instanceof Integer) {
                return arg.toString();
            }
            if (arg instanceof String && arg.toString().matches("\\d+")) {
                return arg.toString();
            }
        }
        
        return null;
    }

    private Long extractPatientIdFromArgs(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Long) {
                return (Long) arg;
            }
            // You might need to check for specific parameter names or types
            // depending on your controller method signatures
        }
        return null;
    }

    private String buildDescription(String className, String methodName, Object[] args) {
        StringBuilder description = new StringBuilder();
        description.append(className).append(".").append(methodName).append("(");
        
        if (args != null && args.length > 0) {
            for (int i = 0; i < Math.min(args.length, 3); i++) { // Limit to first 3 args
                if (i > 0) description.append(", ");
                if (args[i] != null) {
                    String argStr = args[i].toString();
                    if (argStr.length() > 50) {
                        argStr = argStr.substring(0, 47) + "...";
                    }
                    description.append(argStr);
                } else {
                    description.append("null");
                }
            }
            if (args.length > 3) {
                description.append(", ...");
            }
        }
        
        description.append(")");
        return description.toString();
    }
}