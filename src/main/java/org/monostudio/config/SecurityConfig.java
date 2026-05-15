package org.monostudio.config;

import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.servlet.DispatcherType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.monostudio.jpa.repositories.GuestSessionsRepository;
import org.monostudio.security.ContactSubmissionRateLimitFilter;
import org.monostudio.security.JwtGuestAuthenticationFilter;
import org.monostudio.security.JwtLoginAuthenticationFilter;
import org.monostudio.security.JwtTokenVerifierFilter;
import org.monostudio.security.MaintenanceModeFilter;
import org.monostudio.security.oauth2.GoogleOAuth2AuthenticationFailureHandler;
import org.monostudio.security.oauth2.GoogleOAuth2AuthenticationSuccessHandler;
import org.monostudio.security.services.JwtTokenService;
import org.monostudio.security.services.AuthorizationHeaderParserService;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpMethod;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final UserDetailsService userDetailsService;
    private final SecretKey secretKey;
    private final SecurityProperties securityProperties;
    private final AuthorizationHeaderParserService<Claims> jwtClaimsParserService;
    private final GuestSessionsRepository guestSessionsRepository;
    private final CorsProperties corsProperties;
    private final RateLimitConfig rateLimitConfig;
    private final JwtTokenService jwtTokenService;
    private final GoogleOAuth2AuthenticationSuccessHandler googleOAuth2AuthenticationSuccessHandler;
    private final GoogleOAuth2AuthenticationFailureHandler googleOAuth2AuthenticationFailureHandler;
    private final PasswordEncoder passwordEncoder;
    private final ContactSubmissionRateLimitFilter contactSubmissionRateLimitFilter;
    private final MaintenanceModeFilter maintenanceModeFilter;
    private final JwtTokenVerifierFilter jwtTokenVerifierFilter;

    @Autowired
    public SecurityConfig(UserDetailsService userDetailsService,
                          SecretKey secretKey,
                          SecurityProperties securityProperties,
                          AuthorizationHeaderParserService<Claims> jwtClaimsParserService,
                          GuestSessionsRepository guestSessionsRepository,
                          CorsProperties corsProperties,
                          RateLimitConfig rateLimitConfig,
                          JwtTokenService jwtTokenService,
                          GoogleOAuth2AuthenticationSuccessHandler googleOAuth2AuthenticationSuccessHandler,
                          GoogleOAuth2AuthenticationFailureHandler googleOAuth2AuthenticationFailureHandler,
                          PasswordEncoder passwordEncoder,
                          ContactSubmissionRateLimitFilter contactSubmissionRateLimitFilter,
                          MaintenanceModeFilter maintenanceModeFilter,
                          JwtTokenVerifierFilter jwtTokenVerifierFilter) {
        this.userDetailsService = userDetailsService;
        this.secretKey = secretKey;
        this.securityProperties = securityProperties;
        this.jwtClaimsParserService = jwtClaimsParserService;
        this.guestSessionsRepository = guestSessionsRepository;
        this.corsProperties = corsProperties;
        this.rateLimitConfig = rateLimitConfig;
        this.jwtTokenService = jwtTokenService;
        this.googleOAuth2AuthenticationSuccessHandler = googleOAuth2AuthenticationSuccessHandler;
        this.googleOAuth2AuthenticationFailureHandler = googleOAuth2AuthenticationFailureHandler;
        this.passwordEncoder = passwordEncoder;
        this.contactSubmissionRateLimitFilter = contactSubmissionRateLimitFilter;
        this.maintenanceModeFilter = maintenanceModeFilter;
        this.jwtTokenVerifierFilter = jwtTokenVerifierFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(
        HttpSecurity httpSecurity,
        AuthenticationManager authenticationManager
    ) throws Exception {
        // Do not call httpSecurity.authenticationManager(...) here: our @Bean manager only has
        // DaoAuthenticationProvider (for JWT login/guest filters). oauth2Login() needs its own
        // providers (OAuth2LoginAuthenticationProvider etc.); forcing a Dao-only manager breaks Google login.
        return httpSecurity
            .headers(configure -> configure.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(configure -> configure.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(configure -> configure
                .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.ASYNC, DispatcherType.FORWARD).permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/actuator/info").permitAll()
                .requestMatchers("/webhook/shipping/**").permitAll()
                .requestMatchers("/api/public/cart", "/api/public/cart/**").permitAll()
                .requestMatchers("/api/public/mock/shipping/**").permitAll()
                .requestMatchers("/api/public/mock/returns/**").permitAll()
                .requestMatchers("/api/public/discount/**").permitAll()
                .requestMatchers("/api/public/shipping/**").permitAll()
                .requestMatchers("/api/public/tracking/**").permitAll()
                .requestMatchers("/api/public/checkout/**").permitAll()
                .requestMatchers("/api/public/receipt", "/api/public/receipt/**").permitAll()
                .requestMatchers("/api/public/blog/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/public/promotions", "/api/public/promotions/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/public/contact").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/public/about", "/api/public/about/**").permitAll()
                .requestMatchers("/api/data/notifications/stream").permitAll()
                .requestMatchers("/api/public/categories", "/api/public/categories/**").permitAll()
                .requestMatchers("/api/public/behavior", "/api/public/behavior/**").permitAll()
                .requestMatchers("/api/public/products/", "/api/public/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/data/products", "/api/data/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/data/product_categories", "/api/data/product_categories/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/data/images/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/data/product-variants", "/api/data/product-variants/**").permitAll()
                .requestMatchers("/api/public/auth/register").permitAll()
                .requestMatchers("/api/public/auth/google/start").permitAll()
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated())
            .oauth2Login(configure -> configure
                .successHandler(googleOAuth2AuthenticationSuccessHandler)
                .failureHandler(googleOAuth2AuthenticationFailureHandler))
            .addFilter(this.loginFilterForUrl("/api/public/auth/login", authenticationManager))
            .addFilterAfter(this.guestFilterForUrl("/api/public/guest", authenticationManager),
                            JwtLoginAuthenticationFilter.class)
            .addFilterAfter(jwtTokenVerifierFilter, JwtGuestAuthenticationFilter.class)
            .addFilterBefore(maintenanceModeFilter, JwtTokenVerifierFilter.class)
            .addFilterBefore(contactSubmissionRateLimitFilter, JwtTokenVerifierFilter.class)
            .cors(corsConfigurer -> corsConfigurer.configurationSource(corsConfigurationSource()))
            .build();
    }


    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(this.daoAuthenticationProvider());
    }

    private DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(this.passwordEncoder);
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }

    private UsernamePasswordAuthenticationFilter loginFilterForUrl(
        String url,
        AuthenticationManager authenticationManager
    ) throws Exception {
        JwtLoginAuthenticationFilter filter = new JwtLoginAuthenticationFilter(
            jwtTokenService,
            authenticationManager,
            rateLimitConfig);
        filter.setFilterProcessesUrl(url);
        return filter;
    }

    private UsernamePasswordAuthenticationFilter guestFilterForUrl(
        String url,
        AuthenticationManager authenticationManager
    ) throws Exception {
        JwtGuestAuthenticationFilter filter = new JwtGuestAuthenticationFilter(
            securityProperties,
            jwtTokenService,
            authenticationManager,
            guestSessionsRepository,
            rateLimitConfig);
        filter.setFilterProcessesUrl(url);
        return filter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> allowedOrigins = Arrays.asList(
            corsProperties.getAllowedOrigins().split(corsProperties.getListDelimiter())
        );
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
