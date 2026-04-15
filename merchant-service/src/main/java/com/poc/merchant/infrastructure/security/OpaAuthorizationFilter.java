package com.poc.merchant.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class OpaAuthorizationFilter extends OncePerRequestFilter {

    private final OpaAuthorizationClient opaAuthorizationClient;

    public OpaAuthorizationFilter(OpaAuthorizationClient opaAuthorizationClient) {
        this.opaAuthorizationClient = opaAuthorizationClient;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(role -> role.replace("ROLE_", "").toLowerCase())
                .toList();

        boolean allowed = opaAuthorizationClient.isAllowed(
                authentication.getName(),
                roles,
                request.getMethod(),
                request.getRequestURI()
        );

        if (!allowed) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "OPA denied access");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
