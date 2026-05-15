package org.monostudio.api.controllers;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import org.monostudio.config.OAuth2LoginProperties;
import org.monostudio.security.oauth2.GoogleOAuth2AuthenticationSuccessHandler;

@RestController
public class PublicGoogleAuthController {
    private final OAuth2LoginProperties oAuth2LoginProperties;

    public PublicGoogleAuthController(OAuth2LoginProperties oAuth2LoginProperties) {
        this.oAuth2LoginProperties = oAuth2LoginProperties;
    }

    @GetMapping("/api/public/auth/google/start")
    public ResponseEntity<Void> startGoogleOAuth2(
        @RequestParam(name = "redirect", required = false) String redirect,
        HttpServletResponse response
    ) {
        String callbackUrl = oAuth2LoginProperties.getFrontendCallbackUrl();
        if (redirect != null && !redirect.isBlank()) {
            callbackUrl = UriComponentsBuilder.fromUriString(callbackUrl)
                .queryParam("redirect", redirect)
                .build(true)
                .toUriString();
        }

        Cookie redirectCookie = new Cookie(
            GoogleOAuth2AuthenticationSuccessHandler.REDIRECT_COOKIE_NAME,
            callbackUrl
        );
        redirectCookie.setHttpOnly(true);
        redirectCookie.setPath("/");
        redirectCookie.setMaxAge(300);
        response.addCookie(redirectCookie);

        return ResponseEntity.status(302)
            .header("Location", "/oauth2/authorization/google")
            .build();
    }
}
