package com.context_mcp.context_mcp.infrastructure.observability;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Assigns a correlation ID to every HTTP request for structured log correlation.
 * If the client sends an {@code X-Correlation-ID} header the provided value is used;
 * otherwise a random UUID is generated.
 */
@Component
@Order(1)
public class CorrelationIdFilter implements Filter {

    private static final String MDC_KEY = "correlationId";
    private static final String HEADER = "X-Correlation-ID";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            String correlationId = null;
            if (request instanceof HttpServletRequest httpRequest) {
                correlationId = httpRequest.getHeader(HEADER);
            }
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }
            MDC.put(MDC_KEY, correlationId);
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
