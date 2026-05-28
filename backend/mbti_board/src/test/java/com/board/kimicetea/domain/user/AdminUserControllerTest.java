package com.board.kimicetea.domain.user;

import com.board.kimicetea.IntegrationTestSupport;
import com.board.kimicetea.domain.auth.dto.LoginRequest;
import com.board.kimicetea.domain.auth.dto.SignupRequest;
import com.board.kimicetea.domain.user.dto.AdminUserRoleRequest;
import com.board.kimicetea.domain.user.dto.AdminUserStateRequest;
import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.enums.UserRole;
import com.board.kimicetea.domain.user.enums.UserState;
import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.global.mail.EmailService;
import com.board.kimicetea.global.security.redis.TokenRedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 어드민 유저 관리 API 통합 테스트 (7-1)
 * 주의: 어드민 권한은 signup 후 DB에서 직접 role을 ADMIN으로 변경하여 테스트
 */
@AutoConfigureMockMvc
class AdminUserControllerTest extends IntegrationTestSupport {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    UserRepository userRepository;
    @Autowired
    TokenRedisService tokenRedisService;
    @Autowired
    StringRedisTemplate redisTemplate;
    @MockBean
    EmailService emailService;

    private static final String ADMIN_EMAIL    = "admin-test@example.com";
    private static final String ADMIN_PASSWORD = "password123!";
    private static final String ADMIN_NICKNAME = "어드민테스트";
    private static final String ADMIN_MBTI     = "INTJ";

    private static final String USER_EMAIL    = "admin-user-test@example.com";
    private static final String USER_PASSWORD = "password123!";
    private static final String USER_NICKNAME = "일반유저테스트";

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });
    }

    // ─────────────────────────────────────────────────────────────
    // 헬퍼
    // ─────────────────────────────────────────────────────────────

    /** 어드민 회원가입 → role 직접 ADMIN 변경 → 로그인 후 AT 반환 */
    private String 어드민_로그인_AT_추출() throws Exception {
        tokenRedisService.markEmailVerified(ADMIN_EMAIL);
        mockMvc.perform(post(SIGNUP_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new SignupRequest(ADMIN_EMAIL, ADMIN_PASSWORD, ADMIN_NICKNAME, ADMIN_MBTI))))
                .andExpect(status().isCreated());

        final User admin = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow();
        admin.changeRole(UserRole.ADMIN);
        userRepository.save(admin);

        final String body = mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new LoginRequest(ADMIN_EMAIL, ADMIN_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).path("data").path("accessToken").asText();
    }

    /** 일반 유저 회원가입 후 UUID 반환 */
    private UUID 유저_회원가입_후_ID_추출() throws Exception {
        tokenRedisService.markEmailVerified(USER_EMAIL);
        mockMvc.perform(post(SIGNUP_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new SignupRequest(USER_EMAIL, USER_PASSWORD, USER_NICKNAME, ADMIN_MBTI))))
                .andExpect(status().isCreated());

        return userRepository.findByEmail(USER_EMAIL).orElseThrow().getId();
    }

    // ─────────────────────────────────────────────────────────────
    // GET /api/admin/users
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("7-1. 어드민 유저 목록 조회 → 200 OK, 페이지 반환")
    void 어드민_유저_목록_조회_성공() throws Exception {
        final String at = 어드민_로그인_AT_추출();
        유저_회원가입_후_ID_추출();

        mockMvc.perform(get(ADMIN_USERS_URL)
                .header("Authorization", "Bearer " + at))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    @DisplayName("7-2. 이메일 필터 조회 → 일치하는 유저만 반환")
    void 어드민_유저_이메일_필터_조회_성공() throws Exception {
        final String at = 어드민_로그인_AT_추출();
        유저_회원가입_후_ID_추출();

        mockMvc.perform(get(ADMIN_USERS_URL)
                .param("email", "admin-user-test")
                .header("Authorization", "Bearer " + at))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].email").value(USER_EMAIL));
    }

    @Test
    @DisplayName("7-3. 역할 필터 조회 → ADMIN 역할만 반환")
    void 어드민_유저_역할_필터_조회_성공() throws Exception {
        final String at = 어드민_로그인_AT_추출();
        유저_회원가입_후_ID_추출();

        mockMvc.perform(get(ADMIN_USERS_URL)
                .param("role", "ADMIN")
                .header("Authorization", "Bearer " + at))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].role").value("ADMIN"));
    }

    @Test
    @DisplayName("7-4. 비인가 유저(USER) 어드민 목록 조회 → 403")
    void 비인가_유저_어드민_목록_조회_실패() throws Exception {
        tokenRedisService.markEmailVerified(USER_EMAIL);
        mockMvc.perform(post(SIGNUP_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new SignupRequest(USER_EMAIL, USER_PASSWORD, USER_NICKNAME, ADMIN_MBTI))))
                .andExpect(status().isCreated());

        final String body = mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(USER_EMAIL, USER_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        final String userAt = objectMapper.readTree(body).path("data").path("accessToken").asText();

        mockMvc.perform(get(ADMIN_USERS_URL)
                .header("Authorization", "Bearer " + userAt))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("7-5. 미인증 요청 → 401")
    void 미인증_어드민_목록_조회_실패() throws Exception {
        mockMvc.perform(get(ADMIN_USERS_URL))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────────────────────────────────────────────
    // PATCH /api/admin/users/{id}/state
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("7-6. 유저 상태 변경 → 200 OK, state 변경 확인")
    void 어드민_유저_상태_변경_성공() throws Exception {
        final String at = 어드민_로그인_AT_추출();
        final UUID targetId = 유저_회원가입_후_ID_추출();

        mockMvc.perform(patch(String.format(ADMIN_USER_STATE_URL, targetId))
                .header("Authorization", "Bearer " + at)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AdminUserStateRequest(UserState.LOCKED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        final User updated = userRepository.findById(targetId).orElseThrow();
        assert updated.getState() == UserState.LOCKED;
    }

    @Test
    @DisplayName("7-7. 자기 자신 상태 변경 시도 → 400")
    void 어드민_자기_자신_상태_변경_실패() throws Exception {
        final String at = 어드민_로그인_AT_추출();
        final UUID adminId = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow().getId();

        mockMvc.perform(patch(String.format(ADMIN_USER_STATE_URL, adminId))
                .header("Authorization", "Bearer " + at)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AdminUserStateRequest(UserState.LOCKED))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("자기 자신의 상태/역할은 변경할 수 없습니다."));
    }

    @Test
    @DisplayName("7-8. 존재하지 않는 유저 상태 변경 → 404")
    void 어드민_존재하지_않는_유저_상태_변경_실패() throws Exception {
        final String at = 어드민_로그인_AT_추출();
        final UUID nonExistId = UUID.randomUUID();

        mockMvc.perform(patch(String.format(ADMIN_USER_STATE_URL, nonExistId))
                .header("Authorization", "Bearer " + at)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AdminUserStateRequest(UserState.LOCKED))))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────────────────────────────────────────────
    // PATCH /api/admin/users/{id}/role
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("7-9. 유저 역할 변경 → 200 OK, role 변경 확인")
    void 어드민_유저_역할_변경_성공() throws Exception {
        final String at = 어드민_로그인_AT_추출();
        final UUID targetId = 유저_회원가입_후_ID_추출();

        mockMvc.perform(patch(String.format(ADMIN_USER_ROLE_URL, targetId))
                .header("Authorization", "Bearer " + at)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AdminUserRoleRequest(UserRole.TESTER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        final User updated = userRepository.findById(targetId).orElseThrow();
        assert updated.getRole() == UserRole.TESTER;
    }

    @Test
    @DisplayName("7-10. 자기 자신 역할 변경 시도 → 400")
    void 어드민_자기_자신_역할_변경_실패() throws Exception {
        final String at = 어드민_로그인_AT_추출();
        final UUID adminId = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow().getId();

        mockMvc.perform(patch(String.format(ADMIN_USER_ROLE_URL, adminId))
                .header("Authorization", "Bearer " + at)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AdminUserRoleRequest(UserRole.USER))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("자기 자신의 상태/역할은 변경할 수 없습니다."));
    }

    @Test
    @DisplayName("7-11. 존재하지 않는 유저 역할 변경 → 404")
    void 어드민_존재하지_않는_유저_역할_변경_실패() throws Exception {
        final String at = 어드민_로그인_AT_추출();
        final UUID nonExistId = UUID.randomUUID();

        mockMvc.perform(patch(String.format(ADMIN_USER_ROLE_URL, nonExistId))
                .header("Authorization", "Bearer " + at)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AdminUserRoleRequest(UserRole.USER))))
                .andExpect(status().isNotFound());
    }
}
