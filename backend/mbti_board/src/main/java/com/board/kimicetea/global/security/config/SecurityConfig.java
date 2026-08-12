package com.board.kimicetea.global.security.config;

import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.global.security.handler.JwtAccessDeniedHandler;
import com.board.kimicetea.global.security.handler.JwtAuthenticationEntryPoint;
import com.board.kimicetea.global.security.oauth2.AppRoleSyncService;
import com.board.kimicetea.global.security.oauth2.AuthorizedClientRefreshService;
import com.board.kimicetea.global.security.oauth2.CustomOAuth2UserService;
import com.board.kimicetea.global.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.board.kimicetea.global.security.oauth2.OAuth2AuthenticationSuccessHandler;
import com.board.kimicetea.global.security.session.SessionAuthenticationFilter;
import com.board.kimicetea.global.security.session.SessionService;
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
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
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

        private final SessionService sessionService;
        private final AuthorizedClientRefreshService authorizedClientRefreshService;
        private final UserRepository userRepository;
        private final AppRoleSyncService appRoleSyncService;
        private final CustomOAuth2UserService customOAuth2UserService;
        private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
        private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
        private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
        private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

        @Value("${frontend.url}")
        private String frontendUrl;

        @Bean
        SecurityFilterChain filterChain(final HttpSecurity http,
                        final ClientRegistrationRepository clientRegistrationRepository) throws Exception {
                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(AbstractHttpConfigurer::disable)
                                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .formLogin(AbstractHttpConfigurer::disable)
                                .httpBasic(AbstractHttpConfigurer::disable)

                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                                                .requestMatchers(
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
                                                .authorizationEndpoint(endpoint -> endpoint
                                                                .authorizationRequestResolver(
                                                                                pkceAuthorizationRequestResolver(clientRegistrationRepository)))
                                                .userInfoEndpoint(u -> u.oidcUserService(customOAuth2UserService))
                                                .successHandler(oAuth2AuthenticationSuccessHandler)
                                                .failureHandler(oAuth2AuthenticationFailureHandler))

                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                                                .accessDeniedHandler(jwtAccessDeniedHandler))

                                .addFilterBefore(
                                                new SessionAuthenticationFilter(
                                                                sessionService, authorizedClientRefreshService, userRepository, appRoleSyncService),
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        /**
         * AS(thebluebutter) client는 confidential(client_secret_basic)이지만 AS가
         * require-proof-key로 모든 client에 PKCE를 강제한다 — Spring Security는 public client에만
         * 기본으로 PKCE를 붙이므로, confidential client에도 강제로 code_challenge를 실어 보내도록 resolver를 커스터마이즈한다(connect_manual.md 참고).
         */
        private OAuth2AuthorizationRequestResolver pkceAuthorizationRequestResolver(final ClientRegistrationRepository clientRegistrationRepository) {
                // Authorization Request에 code_challenge/code_challenge_method 자동 삽입 설정
                final DefaultOAuth2AuthorizationRequestResolver resolver =
                                new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization");
                resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
                return resolver;
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
                config.setAllowCredentials(true); // 세션 쿠키 전송을 위해 필수
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);
                return source;
        }
}
