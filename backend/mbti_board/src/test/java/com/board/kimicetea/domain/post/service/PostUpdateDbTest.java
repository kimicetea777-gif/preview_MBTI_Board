package com.board.kimicetea.domain.post.service;

import com.board.kimicetea.domain.mbti.MbtiMetadataSupport;
import com.board.kimicetea.domain.post.dto.DebatePostCreateRequest;
import com.board.kimicetea.domain.post.dto.DebateStanceRequest;
import com.board.kimicetea.domain.post.dto.PostDetailResponse;
import com.board.kimicetea.domain.post.dto.PostUpdateRequest;
import com.board.kimicetea.domain.user.security.CustomUserDetails;
import com.board.kimicetea.global.config.JpaConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 게시글 수정(PostService.updatePost)의 응답이 상세 조회(buildDetailResponse)와 같은 정보를 담는지 실제 PostgreSQL에서 확인한다.
 * 수정 응답에서 토론글의 "내가 고른 진영(myStance)"이 빠지면, 수정 직후 화면에서 내 선택 표시가 사라질 수 있다.
 * 주의: 실제 DB가 필요하다 — application-test.yaml의 datasource(기본 localhost:5432). 각 테스트는 롤백된다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({JpaConfig.class, PostService.class, MbtiMetadataSupport.class})
class PostUpdateDbTest {

    private static final String CONTENT =
            "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"본문\"}]}]}";
    private static final String NEW_CONTENT =
            "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"수정된 본문\"}]}]}";

    @Autowired private PostService postService;
    @Autowired private JdbcTemplate jdbc;

    // PostService 생성자 의존성 — 회원 글 수정 경로에서는 쓰이지 않는다
    @MockBean private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("토론글 수정: 응답에 내가 고른 진영(myStance)이 유지된다")
    void debateUpdateKeepsMyStance() {
        final CustomUserDetails user = insertUser();
        final PostDetailResponse created = postService.createDebatePost(
                new DebatePostCreateRequest("토론 제목", CONTENT, null, null, null, null),
                user, new MockHttpServletRequest());
        postService.voteDebateStance(created.id(), new DebateStanceRequest((short) 1), user);

        final PostDetailResponse updated = postService.updatePost(
                created.id(), new PostUpdateRequest("수정된 제목", NEW_CONTENT, null), user);

        assertThat(updated.title()).isEqualTo("수정된 제목");
        assertThat(updated.debateResult()).isNotNull();
        assertThat(updated.debateResult().myStance()).isEqualTo((short) 1);
    }

    @Test
    @DisplayName("토론글 수정: 진영을 고르지 않았으면 myStance는 null이다")
    void debateUpdateWithoutStanceIsNull() {
        final CustomUserDetails user = insertUser();
        final PostDetailResponse created = postService.createDebatePost(
                new DebatePostCreateRequest("토론 제목", CONTENT, null, null, null, null),
                user, new MockHttpServletRequest());

        final PostDetailResponse updated = postService.updatePost(
                created.id(), new PostUpdateRequest("수정된 제목", NEW_CONTENT, null), user);

        assertThat(updated.debateResult()).isNotNull();
        assertThat(updated.debateResult().myStance()).isNull();
    }

    /** 글 작성자로 쓸 회원 한 명을 DB에 넣는다(롤백됨) — 나머지 컬럼은 DB 기본값 */
    private CustomUserDetails insertUser() {
        final UUID id = UUID.randomUUID();
        final String suffix = id.toString().substring(0, 8);
        jdbc.update("insert into users (id, email, auth_server_id, nickname, mbti_id) values (?, ?, ?, ?, 'INFP')",
                id, "upd-" + suffix + "@test.local", UUID.randomUUID(), "u" + suffix);
        return new CustomUserDetails(id, "upd-" + suffix + "@test.local", null, "USER", "device-test");
    }
}
