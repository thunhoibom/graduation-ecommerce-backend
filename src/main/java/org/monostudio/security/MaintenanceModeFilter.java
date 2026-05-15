package org.monostudio.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.monostudio.api.services.SystemSettingsService;

import java.io.IOException;
import java.util.Map;

@Component
public class MaintenanceModeFilter extends OncePerRequestFilter {
  private final SystemSettingsService systemSettingsService;
  private final ObjectMapper objectMapper;

  public MaintenanceModeFilter(
    SystemSettingsService systemSettingsService,
    ObjectMapper objectMapper
  ) {
    this.systemSettingsService = systemSettingsService;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain filterChain
  ) throws ServletException, IOException {
    if (!systemSettingsService.readSettings().isMaintenanceMode()
      || !isAffectedRequest(request)
      || isExemptRequest(request)) {
      filterChain.doFilter(request, response);
      return;
    }

    response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    response.setContentType("application/json");
    response.getWriter().write(objectMapper.writeValueAsString(Map.of(
      "code", "MAINTENANCE_MODE",
      "message", "He thong dang bao tri. Vui long thu lai sau.",
      "maintenanceMode", true
    )));
  }

  private static boolean isAffectedRequest(HttpServletRequest request) {
    String uri = request.getRequestURI();
    return uri != null && uri.startsWith("/api/public/");
  }

  private static boolean isExemptRequest(HttpServletRequest request) {
    String uri = request.getRequestURI();
    if (uri == null || uri.isBlank()) {
      return true;
    }

    if (HttpMethod.GET.matches(request.getMethod())
      && (uri.equals("/api/public/about") || uri.startsWith("/api/public/about/"))) {
      return true;
    }

    return uri.startsWith("/actuator/")
      || uri.startsWith("/webhook/")
      || uri.startsWith("/api/public/checkout")
      || uri.equals("/error");
  }
}
