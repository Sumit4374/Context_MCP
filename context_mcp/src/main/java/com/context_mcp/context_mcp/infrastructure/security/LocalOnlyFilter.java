package com.context_mcp.context_mcp.infrastructure.security;

import com.context_mcp.context_mcp.config.ContextPlatformProperties;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Rejects non-loopback requests when {@code context-platform.server.local-only=true}.
 */
@Component
@Order(2)
public class LocalOnlyFilter implements Filter {

    private final ContextPlatformProperties properties;

    public LocalOnlyFilter(ContextPlatformProperties properties) {
        this.properties = properties;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (properties.getServer().isLocalOnly() && request instanceof HttpServletRequest) {
            String remoteAddr = request.getRemoteAddr();
            if (!isLoopback(remoteAddr)) {
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Server is configured for local-only access");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean isLoopback(String addr) {
        return "127.0.0.1".equals(addr) || "0:0:0:0:0:0:0:1".equals(addr) || "::1".equals(addr);
    }
}
