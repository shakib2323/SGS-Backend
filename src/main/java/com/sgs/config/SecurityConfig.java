// package com.sgs.config;

// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.sgs.security.JwtAuthFilter;
// import com.sgs.security.JwtService;
// import com.sgs.security.RestAuthEntryPoint;
// import com.sgs.user.UserRepository;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.http.HttpMethod;
// import org.springframework.security.config.Customizer;
// import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
// import org.springframework.security.config.annotation.web.builders.HttpSecurity;
// import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
// import org.springframework.security.config.http.SessionCreationPolicy;
// import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
// import org.springframework.security.crypto.password.PasswordEncoder;
// import org.springframework.security.web.SecurityFilterChain;
// import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
// import org.springframework.web.cors.CorsConfiguration;
// import org.springframework.web.cors.CorsConfigurationSource;
// import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

// import java.time.Clock;
// import java.util.List;

// @Configuration
// @EnableMethodSecurity
// public class SecurityConfig {

//     @Bean
//     PasswordEncoder passwordEncoder() {
//         return new BCryptPasswordEncoder(12);
//     }

//     @Bean
//     SecurityFilterChain filterChain(HttpSecurity http, JwtService jwt, UserRepository users, AppProperties props,
//                                     ObjectMapper mapper, Clock clock, RestAuthEntryPoint entryPoint) throws Exception {
//         http
//                 // CSRF is handled by SameSite cookie + mandatory custom header (see JwtAuthFilter).
//                 .csrf(AbstractHttpConfigurer::disable)
//                 .cors(cors ->
//                 cors.configurationSource(corsConfigurationSource())
//             )
//                 .cors(Customizer.withDefaults())
//                 .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                 .authorizeHttpRequests(a -> a
//                         .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
//                         .requestMatchers("/api/public/**", "/api/auth/login", "/api/auth/logout").permitAll()
//                         .requestMatchers("/actuator/health", "/actuator/health/**", "/files/**", "/error").permitAll()
//                         .requestMatchers("/api/admin/**").hasRole("ADMIN")
//                         .requestMatchers("/api/employee/**").hasRole("EMPLOYEE")
//                         .requestMatchers("/api/**").authenticated()
//                         .anyRequest().denyAll())
//                 .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint).accessDeniedHandler(entryPoint))
//                 .headers(h -> h
//                         .frameOptions(f -> f.deny())
//                         .contentSecurityPolicy(c -> c.policyDirectives("default-src 'none'; frame-ancestors 'none'")))
//                 .addFilterBefore(new JwtAuthFilter(jwt, users, props, mapper, clock), UsernamePasswordAuthenticationFilter.class);
//         return http.build();
//     }

// //    @Bean
// //    CorsConfigurationSource corsConfigurationSource(AppProperties props) {
// //        CorsConfiguration c = new CorsConfiguration();
// //        c.setAllowedOrigins(props.cors().allowedOrigins().stream()
// //                .map(String::trim).filter(s -> !s.isEmpty()).toList());
// //        c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
// //        c.setAllowedHeaders(List.of("Content-Type", "X-Requested-With", "Accept","Authorization",
// //        	    "X-CSRF-Token"));
// //        c.setAllowCredentials(true);
// //        c.setMaxAge(3600L);
// //        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
// //        source.registerCorsConfiguration("/api/**", c);
// //        return source;
// //    }

// @Bean
// CorsConfigurationSource corsConfigurationSource() {
//     CorsConfiguration c = new CorsConfiguration();

//     c.setAllowedOrigins(List.of(
//         "http://localhost:5500",
//         "http://127.0.0.1:5500",
//         "http://localhost:5501",
//         "http://127.0.0.1:5501",
//         "http://localhost:3000"
//     ));

//     c.setAllowedMethods(List.of(
//         "GET", "POST", "PUT", "PATCH",
//         "DELETE", "OPTIONS"
//     ));

//     c.setAllowedHeaders(List.of(
//         "Content-Type",
//         "X-Requested-With",
//         "Accept",
//         "Authorization"
//     ));

//     c.setAllowCredentials(true);
//     c.setMaxAge(3600L);

//     UrlBasedCorsConfigurationSource source =
//         new UrlBasedCorsConfigurationSource();

//     source.registerCorsConfiguration("/**", c);

//     return source;
// }

// }


import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Value("${app.cors.allowed-origins:}")
    private String allowedOrigins;

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(
            Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList()
        );

        config.setAllowedMethods(
            Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );

        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    SecurityFilterChain authSecurityFilterChain(HttpSecurity http)
            throws Exception {

        return http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/api/public/**",
                    "/files/**",
                    "/actuator/health",
                    "/actuator/info",
                    "/api/v1/auth/me"
                ).permitAll()
                .requestMatchers(HttpMethod.POST,
                    "/api/public/contact",
                    "/api/v1/auth/register",
                    "/api/v1/auth/register-seller",
                    "/api/v1/auth/login"
                ).permitAll()
                .anyRequest().denyAll()
            )
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .build();
    }
}

