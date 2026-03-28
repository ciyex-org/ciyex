package org.ciyex.ehr.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ciyex.ehr.fhir.GenericFhirResourceService;
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

    private final GenericFhirResourceService fhirService;
    private final ObjectMapper objectMapper = new ObjectMapper();

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

                    Integer timeoutMinutes = getUserTimeoutMinutes();

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

                    userLastActivity.put(username, currentTime);
                }
            } catch (Exception e) {
                log.error("Error processing session timeout", e);
            }
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    private Integer getUserTimeoutMinutes() {
        Integer cached = userTimeoutCache.get("timeout_default");
        if (cached != null) {
            return cached;
        }

        try {
            Map<String, Object> result = fhirService.listAll("practice", 0, 1);
            List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
            if (content != null && !content.isEmpty()) {
                Map<String, Object> practice = content.get(0);

                Integer timeout = asInt(practice.get("tokenExpiryMinutes"));
                if (timeout == null) timeout = asInt(practice.get("practiceSettings.tokenExpiryMinutes"));
                if (timeout == null) timeout = asInt(practice.get("practiceSettings.sessionTimeoutMinutes"));

                if (timeout != null && timeout > 0) {
                    userTimeoutCache.put("timeout_default", timeout);
                    return timeout;
                }
            }
        } catch (Exception e) {
            log.debug("Could not obtain default practice timeout", e);
        }

        return 5; // Default 5 minutes
    }

    private Integer asInt(Object val) {
        if (val instanceof Number n) return n.intValue();
        if (val instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJwtPayload(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) return Map.of();
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
