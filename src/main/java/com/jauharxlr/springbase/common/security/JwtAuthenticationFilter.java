package com.jauharxlr.springbase.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    @org.springframework.beans.factory.annotation.Value("${springbase.anon-key}")
    private String anonKey;

    @org.springframework.beans.factory.annotation.Value("${springbase.service-role-key}")
    private String serviceRoleKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        final String apiKeyHeader = request.getHeader("apikey");

        String userId = null;
        String role = null;
        String projectRef = "default";
        String companyId = null;
        String tenantId = null;

        try {
            // 1. Check for Service Role Key (Admin override)
            if (serviceRoleKey != null && serviceRoleKey.equals(apiKeyHeader)) {
                role = "SERVICE_ROLE";
                userId = "00000000-0000-0000-0000-000000000000";
            } 
            // 2. Check for JWT
            else if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);
                if (jwtUtils.validateToken(jwt)) {
                    userId = jwtUtils.extractUserId(jwt);
                    role = jwtUtils.extractRole(jwt);
                    projectRef = jwtUtils.extractProjectRef(jwt);
                    companyId = jwtUtils.extractCompanyId(jwt);
                    tenantId = jwtUtils.extractTenantId(jwt);
                }
            }
            // 3. Check for Anon Key
            else if (anonKey != null && anonKey.equals(apiKeyHeader)) {
                role = "ANON";
                userId = "anonymous";
            }

            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userId, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())));
                
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                request.setAttribute("project_ref", projectRef);
                if (companyId != null) request.setAttribute("company_id", companyId);
                if (tenantId != null) request.setAttribute("tenant_id", tenantId);
                
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception e) {
            logger.error("Security filter error: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
