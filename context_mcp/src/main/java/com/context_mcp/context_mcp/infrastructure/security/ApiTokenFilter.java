package com.context_mcp.context_mcp.infrastructure.security;

import com.context_mcp.context_mcp.config.ContextPlatformProperties;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Optional API token authentication for HTTP endpoints.
 * Reads token from {@code Authorization: Bearer <token>} header.
 * Token value is read from the {@code CONTEXT_MCP_API_TOKEN} environment variable.
 * Skipped when {@code context-platform.security.api-token-enabled=false}.
 * Actuator and MCP health endpoints are exempted.
 */
@Component
@Order(3)
public class ApiTokenFilter implements Filter {

    private final ContextPlatformProperties properties;
    private final String expectedToken;

    public ApiTokenFilter(ContextPlatformProperties properties) {
        this.properties = properties;
        this.expectedToken = System.getenv("CONTEXT_MCP_API_TOKEN");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!properties.getSecurity().isApiTokenEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        if (request instanceof HttpServletRequest httpRequest) {
            String path = httpRequest.getRequestURI();

            // Exempt health/actuator/openapi endpoints
            if (path.startsWith("/actuator") || path.equals("/api/v1/health") ||
                path.startsWith("/api-docs") || path.startsWith("/swagger-ui") ||
                path.startsWith("/mcp")) {
                chain.doFilter(request, response);
                return;
            }

            if (path.startsWith("/api/")) {
                String authHeader = httpRequest.getHeader("Authorization");
                if (expectedToken != null && !expectedToken.isBlank()) {
                    if (authHeader == null || !authHeader.equals("Bearer " + expectedToken)) {
                        ((HttpServletResponse) response).sendError(
                                HttpServletResponse.SC_UNAUTHORIZED, "Invalid or missing API token");
                        return;
                    }
                }
            }
        }

        chain.doFilter(request, response);
    }
}
