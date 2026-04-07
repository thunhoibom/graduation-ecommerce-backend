package org.monostudio.api.controllers;

import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.monostudio.api.models.AuthorizedAccessPojo;
import org.monostudio.common.services.RegexMatcherAdapterService;
import org.monostudio.security.services.AuthorizationHeaderParserService;
import org.monostudio.security.services.AuthorizedApiService;

import java.util.Collection;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.monostudio.config.Constants.JWT_PREFIX;

@RestController
@RequestMapping("/api/access")
@Tag(name = "User Accounts")
@PreAuthorize("isAuthenticated()")
public class AccessController {
    private final AuthorizationHeaderParserService<Claims> jwtClaimsParserService;
    private final UserDetailsService userDetailsService;
    private final AuthorizedApiService authorizedApiService;
    private final RegexMatcherAdapterService regexMatcherService;

    @Autowired
    public AccessController(
        AuthorizationHeaderParserService<Claims> jwtClaimsParserService,
        UserDetailsService userDetailsService,
        AuthorizedApiService authorizedApiService,
        RegexMatcherAdapterService regexMatcherService
    ) {
        this.jwtClaimsParserService = jwtClaimsParserService;
        this.userDetailsService = userDetailsService;
        this.authorizedApiService = authorizedApiService;
        this.regexMatcherService = regexMatcherService;
    }

    @GetMapping
    @Operation(summary = "List authorized API routes")
    public AuthorizedAccessPojo getApiRoutesAccess(@RequestHeader HttpHeaders requestHeaders)
        throws UsernameNotFoundException, IllegalStateException {
        UserDetails userDetails = this.getUserDetails(requestHeaders);
        if (userDetails==null) {
            return null;
        }
        Collection<String> routes = authorizedApiService.getAuthorizedApiRoutes(userDetails);
        return AuthorizedAccessPojo.builder()
            .routes(routes)
            .build();
    }

    @GetMapping("/{apiRoute}")
    @Operation(summary = "List authorized access to API route")
    public AuthorizedAccessPojo getApiResourceAccess(
        @RequestHeader HttpHeaders requestHeaders,
        @PathVariable String apiRoute)
        throws IllegalStateException {
        UserDetails userDetails = this.getUserDetails(requestHeaders);
        if (userDetails==null) {
            return null;
        }
        Collection<String> permissions = authorizedApiService.getAuthorizedApiRouteAccess(userDetails, apiRoute);
        return AuthorizedAccessPojo.builder()
            .permissions(permissions)
            .build();
    }

    @ResponseStatus(UNAUTHORIZED)
    @ExceptionHandler({UsernameNotFoundException.class, IllegalStateException.class})
    public void handleException(Exception ex) {
    /*
      bad credentials method. whatever provided data is in token, didn't match with existing records of users.
      the consumer sent an invalid token. don't return an explanation of this. the status code should suffice.
      */
    }

    private UserDetails getUserDetails(HttpHeaders requestHeaders)
        throws UsernameNotFoundException, IllegalStateException {
        String authorizationHeader = jwtClaimsParserService.extractAuthorizationHeader(requestHeaders);
        if (authorizationHeader==null || !regexMatcherService.isAValidAuthorizationHeader(authorizationHeader)) {
            return null;
        }
        String jwt = authorizationHeader.replace(JWT_PREFIX, "");
        Claims body = jwtClaimsParserService.parseToken(jwt);
        String username = body.getSubject();
        return userDetailsService.loadUserByUsername(username);
    }
}
