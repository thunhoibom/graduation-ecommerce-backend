package org.monostudio.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.monostudio.api.models.LoginPojo;
import org.monostudio.config.SecurityProperties;

import javax.crypto.SecretKey;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class JwtLoginAuthenticationFilter
    extends GenericJwtAuthenticationFilter {
    private final AuthenticationManager authenticationManager;

    public JwtLoginAuthenticationFilter(
        SecurityProperties jwtProperties,
        SecretKey secretKey,
        AuthenticationManager authenticationManager
    ) {
        super(jwtProperties, secretKey);
        this.authenticationManager = authenticationManager;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
        throws AuthenticationException {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return null;
        } else {
            try {
                LoginPojo userData = new ObjectMapper().readValue(request.getInputStream(), LoginPojo.class);
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userData.getName(),
                    userData.getPassword());
                return authenticationManager.authenticate(authentication);
            } catch (IOException e) {
                throw new BadCredentialsException("Request body is not a login request");
            }
        }
    }
}
