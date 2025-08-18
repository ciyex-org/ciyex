package com.qiaben.ciyex.interceptor;

import com.qiaben.ciyex.dto.integration.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RequestContextInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        RequestContext ctx = new RequestContext();
        ctx.setAuthToken(request.getHeader("Authorization"));
        ctx.setOrgId(request.getHeader("X-Org-Id") != null ? Long.valueOf(request.getHeader("X-Org-Id")) : null);
        ctx.setFacilityId(request.getHeader("X-Facility-Id") != null ? Long.valueOf(request.getHeader("X-Facility-Id")) : null);
        ctx.setRole(request.getHeader("X-Role"));
        RequestContext.set(ctx);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        RequestContext.clear();
    }
}

/*

How to use RequestContext in your service or controller:

RequestContext ctx = RequestContext.get();
Long orgId = ctx.getOrgId();
String role = ctx.getRole();
*/

