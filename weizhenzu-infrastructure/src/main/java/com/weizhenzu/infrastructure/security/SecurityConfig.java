package com.weizhenzu.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weizhenzu.common.constant.CommonConstants;
import com.weizhenzu.common.context.UserContext;
import com.weizhenzu.common.enums.UserTypeEnum;
import com.weizhenzu.common.result.Result;
import com.weizhenzu.common.result.ResultCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

/**
 * Spring Security 配置
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.setAllowCredentials(true);
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setExposedHeaders(Arrays.asList(CommonConstants.AUTH_HEADER, CommonConstants.TRACE_ID_HEADER));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/user/auth/**",
                    "/api/user/merchants/**",
                    "/api/user/dishes/**",
                    "/api/user/categories/**",
                    "/api/user/recommend/**",
                    "/api/user/coupons/available",
                    "/api/merchant/auth/**",
                    "/api/rider/auth/**",
                    "/api/admin/auth/**",
                    "/api/public/**",
                    "/ws/**",
                    "/doc.html", "/webjars/**", "/v3/api-docs/**", "/swagger-ui/**",
                    "/actuator/**", "/favicon.ico"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(new JwtAuthFilter(jwtUtils, objectMapper),
                    UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * JWT 认证过滤器
     */
    public static class JwtAuthFilter extends OncePerRequestFilter {

        private final JwtUtils jwtUtils;
        private final ObjectMapper objectMapper;

        public JwtAuthFilter(JwtUtils jwtUtils, ObjectMapper objectMapper) {
            this.jwtUtils = jwtUtils;
            this.objectMapper = objectMapper;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp,
                                        FilterChain chain) throws ServletException, IOException {
            String header = req.getHeader(CommonConstants.AUTH_HEADER);
            if (header != null && header.startsWith(CommonConstants.TOKEN_PREFIX)) {
                try {
                    var claims = jwtUtils.parse(header.substring(CommonConstants.TOKEN_PREFIX.length()));
                    Long userId = Long.valueOf(claims.getSubject());
                    Integer userType = claims.get("userType", Integer.class);

                    var ctx = new UserContext.LoginUser();
                    ctx.setId(userId);
                    ctx.setUserType(UserTypeEnum.of(userType));
                    UserContext.set(ctx);

                    var auth = new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } catch (Exception e) {
                    writeError(resp, ResultCode.UNAUTHORIZED);
                    return;
                }
            }
            try {
                chain.doFilter(req, resp);
            } finally {
                UserContext.clear();
            }
        }

        private void writeError(HttpServletResponse resp, ResultCode rc) throws IOException {
            resp.setStatus(200);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write(objectMapper.writeValueAsString(Result.fail(rc)));
        }
    }
}
