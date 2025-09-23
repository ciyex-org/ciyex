package com.qiaben.ciyex.audit;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletRequest;

import java.lang.reflect.Method;

@Aspect
@Component
public class AuditLogAspect {

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private HttpServletRequest request;

    // Log controller methods
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
            Method method = signature.getMethod();
            String methodName = method.getName();
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

    // Log service methods for create/update/delete operations
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

    private String getClientIpAddress() {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }

        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }

        return ipAddress;
    }

    private String determineActionType(String methodName, String className) {
        if (methodName.toLowerCase().contains("login")) {
            return "LOGIN";
        }
        if (methodName.toLowerCase().contains("logout")) {
            return "LOGOUT";
        }
        if (methodName.toLowerCase().contains("get") || methodName.toLowerCase().contains("find")) {
            return "VIEW";
        }
        if (methodName.toLowerCase().contains("download")) {
            return "DOWNLOAD";
        }
        if (methodName.toLowerCase().contains("create") || methodName.toLowerCase().contains("save")) {
            return "CREATE";
        }
        if (methodName.toLowerCase().contains("update")) {
            return "UPDATE";
        }
        if (methodName.toLowerCase().contains("delete") || methodName.toLowerCase().contains("remove")) {
            return "DELETE";
        }
        if (methodName.toLowerCase().contains("send")) {
            return "SEND";
        }
        if (methodName.toLowerCase().contains("reply")) {
            return "REPLY";
        }
        return "EXECUTE";
    }

    private String determineEntityType(String className) {
        if (className.toLowerCase().contains("patient")) {
            return "PATIENT";
        }
        if (className.toLowerCase().contains("appointment")) {
            return "APPOINTMENT";
        }
        if (className.toLowerCase().contains("message")) {
            return "MESSAGE";
        }
        if (className.toLowerCase().contains("doctor")) {
            return "DOCTOR";
        }
        if (className.toLowerCase().contains("user")) {
            return "USER";
        }
        if (className.toLowerCase().contains("lab")) {
            return "LAB_ORDER";
        }
        if (className.toLowerCase().contains("vital")) {
            return "VITAL";
        }
        if (className.toLowerCase().contains("insurance")) {
            return "INSURANCE";
        }
        if (className.toLowerCase().contains("immunization")) {
            return "IMMUNIZATION";
        }
        return className.replace("Controller", "").replace("Service", "").toUpperCase();
    }

    private String extractEntityId(JoinPoint joinPoint, Object result) {
        try {
            Object[] args = joinPoint.getArgs();
            for (Object arg : args) {
                if (arg instanceof Long) {
                    return arg.toString();
                }
                if (arg instanceof String && ((String) arg).matches("\\d+")) {
                    return (String) arg;
                }
            }

            if (result != null) {
                if (result instanceof Long) {
                    return result.toString();
                }
                try {
                    java.lang.reflect.Method getIdMethod = result.getClass().getMethod("getId");
                    Object idValue = getIdMethod.invoke(result);
                    if (idValue != null) {
                        return idValue.toString();
                    }
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    private String buildDescription(String className, String methodName, Object[] args) {
        String action = determineActionType(methodName, className);
        String entity = determineEntityType(className);
        return action + " operation on " + entity + " via " + methodName + " method";
    }
}