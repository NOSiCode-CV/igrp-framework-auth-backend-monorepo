package cv.igrp.framework.auth.core.route;

import cv.igrp.framework.auth.core.audit.AgentIdentity;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/** Counts authenticated incoming calls once, including authorization denials. */
public final class AgentCallMetricsFilter extends OncePerRequestFilter {
    private final MeterRegistry registry;
    private final String service;

    public AgentCallMetricsFilter(MeterRegistry registry, String service) {
        this.registry = registry;
        this.service = service;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String agent = AgentIdentity.agent(SecurityContextHolder.getContext().getAuthentication());
        if (agent != null && !agent.isBlank()) {
            registry.counter("igrp.agent.calls", "service", service, "agent", agent).increment();
        }
        chain.doFilter(request, response);
    }
}
