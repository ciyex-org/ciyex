package org.ciyex.ehr.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ciyex.ehr.service.PracticeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionTimeoutInterceptor implements HandlerInterceptor {

    private final PracticeService practiceService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Store last activity time for each user
    private final Map<String, Long> userLastActivity = new ConcurrentHashMap<>();
    private final Map<String, Integer> userTimeoutCache = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            try {
                Map<String, Object> claims = parseJwtPayload(token);
                String username = (String) claims.getOrDefault("preferred_username", claims.get("email"));
                
                if (username != null) {
                    long currentTime = System.currentTimeMillis();
                    Long lastActivity = userLastActivity.get(username);
                    
                    // Get session timeout for user's practice
                    Integer timeoutMinutes = getUserTimeoutMinutes(claims);
                    
                    // Check if session expired
                    if (lastActivity != null) {
                        long timeSinceActivity = currentTime - lastActivity;
                        long timeoutMs = timeoutMinutes * 60 * 1000L;
                        
                        if (timeSinceActivity > timeoutMs) {
                            log.info("Session expired for user: {} after {} minutes", username, timeoutMinutes);
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"success\":false,\"message\":\"Session expired\",\"errorCode\":\"SESSION_EXPIRED\"}");
                            return false;
                        }
                    }
                    
                    // Update last activity
                    userLastActivity.put(username, currentTime);
                }
            } catch (Exception e) {
                log.error("Error processing session timeout", e);
            }
        }
        
        return true;
    }

    private Integer getUserTimeoutMinutes(Map<String, Object> claims) {
        try {
            @SuppressWarnings("unchecked")
            List<String> groups = (List<String>) claims.getOrDefault("groups", List.of());
            
            for (String group : groups) {
                if (group.startsWith("practice_")) {
                    String practiceId = group.substring("practice_".length());
                    String cacheKey = "timeout_" + practiceId;
                    
                    Integer cached = userTimeoutCache.get(cacheKey);
                    if (cached != null) {
                        return cached;
                    }
                    
                    try {
                        Long id = Long.parseLong(practiceId);
                        var practice = practiceService.getById(id);
                        if (practice != null) {
                            Integer timeout = null;
                            if (practice.getPracticeSettings() != null) {
                                timeout = practice.getPracticeSettings().getTokenExpiryMinutes();
                            }
                            if (timeout == null) {
                                timeout = practice.getTokenExpiryMinutes();
                            }
                            if (timeout == null) {
                                if (practice.getPracticeSettings() != null) {
                                    timeout = practice.getPracticeSettings().getSessionTimeoutMinutes();
                                }
                            }
                            if (timeout != null && timeout > 0) {
                                userTimeoutCache.put(cacheKey, timeout);
                                return timeout;
                            }
                        }
                    } catch (Exception e) {
                        log.debug("Could not get timeout for practice: {}", practiceId);
                    }
                }
            }

            // If no practice_* group found, attempt to use the first practice settings
            Integer defaultCached = userTimeoutCache.get("timeout_default");
            if (defaultCached != null) {
                return defaultCached;
            }

            try {
                var practicesResp = practiceService.getAllPractices();
                if (practicesResp != null && practicesResp.getData() != null && !practicesResp.getData().isEmpty()) {
                    var first = practicesResp.getData().get(0);
                    Integer timeout = null;
                    if (first.getPracticeSettings() != null) {
                        timeout = first.getPracticeSettings().getTokenExpiryMinutes();
                    }
                    if (timeout == null) timeout = first.getTokenExpiryMinutes();
                    if (timeout == null && first.getPracticeSettings() != null) timeout = first.getPracticeSettings().getSessionTimeoutMinutes();
                    if (timeout != null && timeout > 0) {
                        userTimeoutCache.put("timeout_default", timeout);
                        return timeout;
                    }
                }
            } catch (Exception e) {
                log.debug("Could not obtain default practice timeout", e);
            }
        } catch (Exception e) {
            log.error("Error getting user timeout", e);
        }
        
        return 5; // Default 5 minutes
    }

    private Map<String, Object> parseJwtPayload(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) {
                return Map.of();
            }
            
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            return objectMapper.readValue(payload, Map.class);
        } catch (Exception e) {
            log.error("Failed to parse JWT payload", e);
            return Map.of();
        }
    }

    public void clearUserSession(String username) {
        userLastActivity.remove(username);
    }
}