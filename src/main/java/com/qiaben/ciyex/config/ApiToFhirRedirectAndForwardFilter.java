package com.qiaben.ciyex.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles API↔FHIR redirect and forward.
 * Transparently preserves whichever Org header the client sends.
 */
@Component
@Order(1)
public class ApiToFhirRedirectAndForwardFilter implements Filter {

    private static final int SC_PERMANENT_REDIRECT = 308;

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String uri = req.getRequestURI();

        // Detect both styles (case-insensitive)
        String orgIdHeaderName = null;
        String orgIdValue = null;

        if (req.getHeader("X-Org-Id") != null) {
            orgIdHeaderName = "X-Org-Id";
            orgIdValue = req.getHeader("X-Org-Id");
        } else if (req.getHeader("orgId") != null) {
            orgIdHeaderName = "orgId";
            orgIdValue = req.getHeader("orgId");
        }

        if (orgIdValue != null && !orgIdValue.isBlank()) {
            System.out.println("[ApiToFhirFilter] Org header detected: " 
                + orgIdHeaderName + " = " + orgIdValue);
            // Preserve it transparently (same header name and value)
            res.setHeader(orgIdHeaderName, orgIdValue);
        } else {
            System.out.println("[ApiToFhirFilter] No org header found — continuing normally.");
        }

        // Case 1: Redirect /api/... → /api/fhir/...
        if (uri.startsWith("/api/") && !uri.startsWith("/api/fhir/")) {
            String newUri = uri.replaceFirst("/api/", "/api/fhir/");
            if (req.getQueryString() != null) {
                newUri += "?" + req.getQueryString();
            }
            System.out.println("[Redirect] " + uri + " → " + newUri);
            res.setStatus(SC_PERMANENT_REDIRECT);
            res.setHeader("Location", newUri);
            return;
        }

        // Case 2: Forward /api/fhir/... → /api/...
        if (uri.startsWith("/api/fhir/")) {
            String forwardUri = uri.replaceFirst("/api/fhir/", "/api/");
            if (req.getQueryString() != null) {
                forwardUri += "?" + req.getQueryString();
            }
            System.out.println("[Forward] " + uri + " → " + forwardUri +
                    (orgIdHeaderName != null ? " [" + orgIdHeaderName + ": " + orgIdValue + "]" : ""));
            req.getRequestDispatcher(forwardUri).forward(req, res);
            return;
        }

        // Default: continue normal chain
        chain.doFilter(request, response);
    }
}
