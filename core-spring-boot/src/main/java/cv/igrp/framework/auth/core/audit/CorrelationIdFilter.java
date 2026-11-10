package cv.igrp.framework.auth.core.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Binds one correlation id to each request (v3-24): in the log MDC under
 * {@link CorrelationIds#MDC_KEY}, as a request attribute, and in the
 * {@code X-Correlation-ID} response header. Calls the service makes to AM while
 * serving the request then carry the same id (see {@link CorrelationIds#outgoingHeaders}).
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    /** Request attribute holding the id, for code that has the request but not the MDC. */
    public static final String REQUEST_ATTRIBUTE = CorrelationIds.MDC_KEY;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String id = CorrelationIds.resolve(request.getHeader(CorrelationIds.HEADER));
        request.setAttribute(REQUEST_ATTRIBUTE, id);
        response.setHeader(CorrelationIds.HEADER, id);
        MDC.put(CorrelationIds.MDC_KEY, id);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(CorrelationIds.MDC_KEY);
        }
    }
}
