package com.db2api.config;

import com.db2api.persistent.log.RequestLog;
import com.db2api.repository.log.RequestLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;

/**
 * Filter that logs every incoming HTTP request, its processing duration,
 * and persists the log entry to the database for monitoring and analytics.
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private final RequestLogRepository requestLogRepository;

    /**
     * Constructs the RequestLoggingFilter with the request log repository.
     *
     * @param requestLogRepository the repository for persisting request logs
     */
    public RequestLoggingFilter(RequestLogRepository requestLogRepository) {
        this.requestLogRepository = requestLogRepository;
    }

    /**
     * Intercepts the request to log details, timing, and persist to DB.
     * 
     * @param request     the incoming HTTP request
     * @param response    the HTTP response
     * @param filterChain the filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Request: {} {} - Status: {} - Duration: {}ms",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    duration);

            // Persist request log to DB for admin dashboard monitoring
            try {
                RequestLog logEntry = new RequestLog();
                logEntry.setHttpMethod(request.getMethod());
                logEntry.setRequestUri(request.getRequestURI());
                logEntry.setStatusCode(response.getStatus());
                logEntry.setDurationMs(duration);
                logEntry.setClientIp(request.getRemoteAddr());
                logEntry.setTimestamp(Instant.now());
                requestLogRepository.save(logEntry);
            } catch (Exception e) {
                // Don't let logging failures affect the request
                logger.warn("Failed to persist request log", e);
            }
        }
    }
}
