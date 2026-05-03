package org.monostudio.security.oauth2;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.monostudio.api.services.RegistrationService;
import org.monostudio.config.OAuth2LoginProperties;
import org.monostudio.jpa.entities.User;
import org.monostudio.security.services.JwtTokenService;

import java.io.IOException;

@Component
public class GoogleOAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    public static final String REDIRECT_COOKIE_NAME = "oauth2_redirect_uri";

    private final RegistrationService registrationService;
    private final UserDetailsService userDetailsService;
    private final JwtTokenService jwtTokenService;
    private final OAuth2LoginProperties oAuth2LoginProperties;

    public GoogleOAuth2AuthenticationSuccessHandler(
        RegistrationService registrationService,
        UserDetailsService userDetailsService,
        JwtTokenService jwtTokenService,
        OAuth2LoginProperties oAuth2LoginProperties
    ) {
        this.registrationService = registrationService;
        this.userDetailsService = userDetailsService;
        this.jwtTokenService = jwtTokenService;
        this.oAuth2LoginProperties = oAuth2LoginProperties;
    }

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        String email = principal.getAttribute("email");
        Boolean emailVerified = principal.getAttribute("email_verified");
        if (Boolean.FALSE.equals(emailVerified)) {
            redirectWithError(request, response, "email_not_verified");
            return;
        }

        String givenName = principal.getAttribute("given_name");
        String familyName = principal.getAttribute("family_name");
        String sub = principal.getAttribute("sub");
        User user;
        try {
            user = registrationService.findOrCreateGoogleUser(email, givenName, familyName, sub);
        } catch (org.monostudio.common.exceptions.BadInputException e) {
            redirectWithError(request, response, "invalid_google_profile");
            return;
        }
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getName());
        String bearerToken = jwtTokenService.issueBearerToken(user.getName(), userDetails.getAuthorities());

        String redirectUri = resolveRedirectUri(request);
        String finalRedirect = UriComponentsBuilder.fromUriString(redirectUri)
            .queryParam("token", bearerToken)
            .build(true)
            .toUriString();

        clearRedirectCookie(response);
        response.setHeader(HttpHeaders.AUTHORIZATION, bearerToken);
        response.sendRedirect(finalRedirect);
    }

    private void redirectWithError(HttpServletRequest request, HttpServletResponse response, String error) throws IOException {
        String redirectUri = resolveRedirectUri(request);
        String finalRedirect = UriComponentsBuilder.fromUriString(redirectUri)
            .queryParam("error", error)
            .build(true)
            .toUriString();
        clearRedirectCookie(response);
        response.sendRedirect(finalRedirect);
    }

    private String resolveRedirectUri(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (REDIRECT_COOKIE_NAME.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                    return cookie.getValue();
                }
            }
        }
        return oAuth2LoginProperties.getFrontendCallbackUrl();
    }

    private void clearRedirectCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REDIRECT_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
