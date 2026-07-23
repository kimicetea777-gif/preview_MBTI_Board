package com.board.kimicetea.global.security.config;

import com.board.kimicetea.global.security.handler.JwtAccessDeniedHandler;
import com.board.kimicetea.global.security.handler.JwtAuthenticationEntryPoint;
import com.board.kimicetea.global.security.jwt.JwtAuthenticationFilter;
import com.board.kimicetea.global.security.jwt.JwtProvider;
import com.board.kimicetea.global.security.oauth2.CustomOAuth2UserService;
import com.board.kimicetea.global.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.board.kimicetea.global.security.oauth2.OAuth2AuthenticationSuccessHandler;
import com.board.kimicetea.global.security.redis.TokenRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
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

        private final JwtProvider jwtProvider;
        private final TokenRedisService tokenRedisService;
        private final CustomOAuth2UserService customOAuth2UserService;
        private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
        private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
        private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
        private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

        @Value("${frontend.url}")
        private String frontendUrl;

        @Bean
        SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(AbstractHttpConfigurer::disable)
                                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .formLogin(AbstractHttpConfigurer::disable)
                                .httpBasic(AbstractHttpConfigurer::disable)

                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                                                .requestMatchers(
                                                                "/api/auth/signup",
                                                                "/api/auth/login",
                                                                "/api/auth/reissue", // RT Cookie 기반 — 인증 없이 접근해야 함
                                                                "/api/auth/oauth2/signup", // signupToken 기반 — 계정이 아직 없는
                                                                                           // 상태에서 호출됨
                                                                "/api/auth/email-verify/send",
                                                                "/api/auth/email-verify/confirm",
                                                                "/api/auth/find-email",
                                                                "/api/auth/password-reset/request",
                                                                "/api/auth/password-reset/confirm",

                                                                // 토큰 자체가 (userId, email) 페어를 증명 — 발송 시 이미 인증된 요청으로만
                                                                // 발급되므로 확인 시점엔 로그인 불필요
                                                                "/api/users/me/recovery-email/confirm",
                                                                "/oauth2/**",
                                                                "/login/oauth2/**",
                                                                "/swagger-ui/**",
                                                                "/v3/api-docs/**")
                                                .permitAll()
                                                // /me보다 먼저 선언해야 아래 /api/users/* 패턴이 /me를 가로채지 않음
                                                .requestMatchers(HttpMethod.GET,
                                                                "/api/users/me",
                                                                "/api/users/me/stats",
                                                                "/api/users/me/mbti/history",
                                                                "/api/users/me/posts",
                                                                "/api/users/me/posts/preview")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.GET,
                                                                "/api/posts/**",
                                                                "/api/comments/**",
                                                                "/api/users/*")
                                                .permitAll()
                                                // MBTI 검사 제출은 회원 전용 — anyRequest().authenticated()에 위임
                                                .requestMatchers(HttpMethod.POST, "/api/mbti/test/submit")
                                                .authenticated()
                                                // FN 응답 저장: 비회원 포함 허용 (GET stats도 동일)
                                                .requestMatchers(HttpMethod.POST, "/api/mbti/fun/responses")
                                                .permitAll()
                                                // 공지사항 조회는 완전 공개
                                                .requestMatchers(HttpMethod.GET, "/api/notices/active")
                                                .permitAll()
                                                // MBTI 메타데이터·문항 조회는 완전 공개 (GET 전용)
                                                .requestMatchers(HttpMethod.GET, "/api/mbti", "/api/mbti/**")
                                                .permitAll()
                                                // 이미지 업로드: Security는 permitAll — 실제 인증은 X-Internal-Key 헤더로 컨트롤러에서 처리
                                                .requestMatchers(HttpMethod.POST, "/api/images/upload").permitAll()
                                                .requestMatchers(HttpMethod.DELETE, "/api/images").permitAll()
                                                // 게시글·댓글 작성·수정·삭제는 비회원도 허용 (서비스 레이어에서 회원/비회원 분기)
                                                // 투표·토론 게시글 작성은 회원 전용 — anyRequest().authenticated()에 위임
                                                .requestMatchers(HttpMethod.POST, "/api/posts", "/api/posts/*/comments")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.PATCH, "/api/posts/*",
                                                                "/api/posts/*/comments/*")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.DELETE, "/api/posts/*",
                                                                "/api/posts/*/comments/*")
                                                .permitAll()
                                                .anyRequest().authenticated())

                                .oauth2Login(oauth2 -> oauth2
                                                .userInfoEndpoint(u -> u.userService(customOAuth2UserService))
                                                .successHandler(oAuth2AuthenticationSuccessHandler)
                                                .failureHandler(oAuth2AuthenticationFailureHandler))

                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                                                .accessDeniedHandler(jwtAccessDeniedHandler))

                                .addFilterBefore(
                                                new JwtAuthenticationFilter(jwtProvider, tokenRedisService),
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        /**
         * 주의: allowCredentials(true) 사용 시 allowedOrigins에 "*" 불가 — 출처 명시 필수
         * 주의: frontendUrl은 ${frontend.url} 환경변수에서 주입 — 하드코딩 금지
         */
        @Bean
        CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOrigins(List.of(frontendUrl));
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
                config.setAllowedHeaders(List.of("*"));
                config.setAllowCredentials(true); // RT 쿠키 전송을 위해 필수
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);
                return source;
        }
}
