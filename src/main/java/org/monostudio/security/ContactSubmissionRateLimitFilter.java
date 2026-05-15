package org.monostudio.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.monostudio.config.RateLimitConfig;

import java.io.IOException;

/**
 * Rate limits POST {@code /api/public/contact} by client IP (5/minute).
 */
@Component
public class ContactSubmissionRateLimitFilter extends OncePerRequestFilter {

    private static final String CONTACT_PATH = "/api/public/contact";

    private final RateLimitConfig rateLimitConfig;

    public ContactSubmissionRateLimitFilter(RateLimitConfig rateLimitConfig) {
        this.rateLimitConfig = rateLimitConfig;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        if (!CONTACT_PATH.equals(request.getRequestURI()) || !HttpMethod.POST.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        Bucket bucket = rateLimitConfig.contactBucketFor(clientIp);
        if (!bucket.tryConsume(1)) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(new ObjectMapper().writeValueAsString(java.util.Map.of(
                "code", "RATE_CONTACT",
                "message", "Too many contact submissions. Please wait before trying again.",
                "canRetry", true
            )));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
