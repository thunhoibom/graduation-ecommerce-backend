package org.monostudio.security.oauth2;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.monostudio.config.OAuth2LoginProperties;

import java.io.IOException;

@Component
public class GoogleOAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {
    private final OAuth2LoginProperties oAuth2LoginProperties;

    public GoogleOAuth2AuthenticationFailureHandler(OAuth2LoginProperties oAuth2LoginProperties) {
        this.oAuth2LoginProperties = oAuth2LoginProperties;
    }

    @Override
    public void onAuthenticationFailure(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception
    ) throws IOException {
        String redirectUri = oAuth2LoginProperties.getFrontendFailureUrl();
        // Never put raw exception.getMessage() in the query string: it can contain spaces and
        // break UriComponentsBuilder / leak internals. Use a short machine-readable reason only.
        String reason = exception.getClass().getSimpleName();

        String finalRedirect = UriComponentsBuilder.fromUriString(redirectUri)
            .queryParam("error", "google_login_failed")
            .queryParam("reason", reason)
            .build()
            .toUriString();

        Cookie cookie = new Cookie(GoogleOAuth2AuthenticationSuccessHandler.REDIRECT_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        response.sendRedirect(finalRedirect);
    }
}
