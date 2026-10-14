package com.ambulanceservice.config;

import com.ambulanceservice.security.AuthEntryPointJwt;
import com.ambulanceservice.security.AuthTokenFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final UserDetailsService userDetailsService;
  private final AuthEntryPointJwt unauthorizedHandler;
  private final AuthTokenFilter authTokenFilter;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public DaoAuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
    authProvider.setUserDetailsService(userDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder());
    return authProvider;
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
    return authConfig.getAuthenticationManager();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(List.of(
        "http://localhost:*",
        "http://127.0.0.1:*",
        "http://192.168.*:*",
        "http://10.*:*"
    ));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin"));
    configuration.setExposedHeaders(List.of("Authorization"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf.disable())
        .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            // 1. Static Web GUI Resources & HTML Partials (Permits /components/** and /*.html)
            .requestMatchers(
                "/",
                "/index.html",
                "/*.html",
                "/components/**",
                "/js/**",
                "/css/**",
                "/images/**",
                "/favicon.ico"
            ).permitAll()

            // 2. Pre-flight OPTIONS checks
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

            // 3. Public Authentication Endpoints
            .requestMatchers("/api/v1/auth/**").permitAll()

            // 4. Public Emergency Guest Bookings & Live Tracking
            .requestMatchers(HttpMethod.POST, "/api/v1/bookings").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/bookings/*").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/bookings/*/location").permitAll()

            // 5. Public Ingestion Webhooks
            .requestMatchers(HttpMethod.POST, "/api/v1/webhooks/**").permitAll()

            // 6. Swagger & OpenAPI Documentation
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

            // 7. All other endpoints require authentication
            .anyRequest().authenticated()
        );

    http.authenticationProvider(authenticationProvider());
    http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
