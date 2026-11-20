package com.board.kimicetea.domain.post.service;

import com.board.kimicetea.domain.mbti.MbtiMetadataSupport;
import com.board.kimicetea.domain.post.dto.PostDetailResponse;
import com.board.kimicetea.domain.post.dto.VoteOptionRequest;
import com.board.kimicetea.domain.post.dto.VoteOptionResponse;
import com.board.kimicetea.domain.post.dto.VotePostCreateRequest;
import com.board.kimicetea.domain.post.entity.VotePost;
import com.board.kimicetea.domain.post.repository.VotePostRepository;
import com.board.kimicetea.domain.user.security.CustomUserDetails;
import com.board.kimicetea.global.config.JpaConfig;
import jakarta.persistence.EntityManager;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 투표 게시글 작성(PostService.createVotePost)이 실제 PostgreSQL에서 돌려주는 응답을 확인한다.
 * createVotePostMeta는 선택지를 저장한 뒤 DB에서 다시 읽지 않고 메모리의 컬렉션을 채워 반환하므로,
 * 그 응답이 "저장 후 DB에서 새로 읽은 값"과 같은지(ID·순서·득표수·MBTI 연결)를 비교한다.
 * 주의: 실제 DB가 필요하다 — application-test.yaml의 datasource(기본 localhost:5432). 각 테스트는 롤백된다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({JpaConfig.class, PostService.class, MbtiMetadataSupport.class})
class VotePostCreateDbTest {

    private static final String CONTENT =
            "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"본문\"}]}]}";

    @Autowired private PostService postService;
    @Autowired private VotePostRepository votePostRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;

    // PostService 생성자 의존성 — 투표글 작성 경로에서는 쓰이지 않는다
    @MockBean private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("자유 투표: 응답의 선택지가 DB에서 새로 읽은 값과 같다 (ID·요청 순서·득표수 0)")
    void freeVoteResponseMatchesDb() {
        final CustomUserDetails user = insertUser();
        final VotePostCreateRequest request = new VotePostCreateRequest("투표 제목", CONTENT, null,
                List.of(free("셋째"), free("첫째"), free("둘째")), null);

        final PostDetailResponse response = postService.createVotePost(request, user, new MockHttpServletRequest());

        final List<VoteOptionResponse> options = response.voteResult().options();
        assertThat(options).extracting(VoteOptionResponse::optionText).containsExactly("셋째", "첫째", "둘째");
        assertThat(options).extracting(VoteOptionResponse::id).doesNotContainNull().doesNotHaveDuplicates();
        assertThat(options).extracting(VoteOptionResponse::voteCount).containsOnly(0);
        assertThat(options).extracting(VoteOptionResponse::mbtiTargetId).containsOnlyNulls();

        assertThat(reloadOptions(response.id())).isEqualTo(options);
        assertThat(optionRowCount(response.id())).isEqualTo(3);
    }

    @Test
    @DisplayName("MBTI 투표: 응답의 선택지가 DB에서 새로 읽은 값과 같다 (MBTI 연결·요청 순서)")
    void mbtiVoteResponseMatchesDb() {
        final CustomUserDetails user = insertUser();
        final VotePostCreateRequest request = new VotePostCreateRequest("MBTI 투표", CONTENT, null,
                List.of(mbti("INFP"), mbti("ENTJ"), mbti("ISTP")), null);

        final PostDetailResponse response = postService.createVotePost(request, user, new MockHttpServletRequest());

        final List<VoteOptionResponse> options = response.voteResult().options();
        assertThat(options).extracting(VoteOptionResponse::mbtiTargetId).containsExactly("INFP", "ENTJ", "ISTP");
        assertThat(options).extracting(VoteOptionResponse::optionText).containsOnlyNulls();
        assertThat(options).extracting(VoteOptionResponse::id).doesNotContainNull().doesNotHaveDuplicates();
        assertThat(options).extracting(VoteOptionResponse::voteCount).containsOnly(0);

        assertThat(reloadOptions(response.id())).isEqualTo(options);
        assertThat(optionRowCount(response.id())).isEqualTo(3);
    }

    @Test
    @DisplayName("선택지 최대 개수(16): 응답과 DB의 선택지가 순서까지 같다")
    void maxOptionsResponseMatchesDb() {
        final CustomUserDetails user = insertUser();
        final List<VoteOptionRequest> requested = java.util.stream.IntStream.rangeClosed(1, 16)
                .mapToObj(i -> free("선택지" + (17 - i))) // 정렬 순서와 요청 순서가 다르게
                .toList();
        final VotePostCreateRequest request = new VotePostCreateRequest("최대 선택지", CONTENT, null, requested, null);

        final PostDetailResponse response = postService.createVotePost(request, user, new MockHttpServletRequest());

        final List<VoteOptionResponse> options = response.voteResult().options();
        assertThat(options).hasSize(16);
        assertThat(options).extracting(VoteOptionResponse::optionText)
                .containsExactlyElementsOf(requested.stream().map(VoteOptionRequest::optionText).toList());
        assertThat(reloadOptions(response.id())).isEqualTo(options);
    }

    /** 영속성 컨텍스트를 비운 뒤 DB에서 선택지를 새로 읽어 응답 DTO로 변환 — 서비스가 메모리로 채운 값과 비교할 기준 */
    private List<VoteOptionResponse> reloadOptions(final Long postId) {
        entityManager.flush();
        entityManager.clear();
        final VotePost reloaded = votePostRepository.findWithOptionsById(postId).orElseThrow();
        return reloaded.getOptions().stream().map(VoteOptionResponse::from).toList();
    }

    private int optionRowCount(final Long postId) {
        final Integer count = jdbc.queryForObject(
                "select count(*) from vote_post_options where post_id = ?", Integer.class, postId);
        return count == null ? 0 : count;
    }

    private static VoteOptionRequest free(final String text) {
        return new VoteOptionRequest(text, null);
    }

    private static VoteOptionRequest mbti(final String mbtiId) {
        return new VoteOptionRequest(null, mbtiId);
    }

    /** 글 작성자로 쓸 회원 한 명을 DB에 넣는다(롤백됨) — 나머지 컬럼은 DB 기본값 */
    private CustomUserDetails insertUser() {
        final UUID id = UUID.randomUUID();
        final String suffix = id.toString().substring(0, 8);
        jdbc.update("insert into users (id, email, auth_server_id, nickname, mbti_id) values (?, ?, ?, ?, 'INFP')",
                id, "vote-" + suffix + "@test.local", UUID.randomUUID(), "v" + suffix);
        return new CustomUserDetails(id, "vote-" + suffix + "@test.local", null, "USER", "device-test");
    }
}
