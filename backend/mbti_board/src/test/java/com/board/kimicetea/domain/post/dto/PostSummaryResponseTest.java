package com.board.kimicetea.domain.post.dto;

import com.board.kimicetea.domain.mbti.entity.MbtiMetadata;
import com.board.kimicetea.domain.post.entity.Post;
import com.board.kimicetea.domain.post.entity.PostType;
import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.enums.MbtiVerified;
import com.board.kimicetea.domain.user.enums.UserRole;
import com.board.kimicetea.domain.user.enums.UserState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PostSummaryResponseTest {

    @Test
    @DisplayName("작성자가 탈퇴(익명화)해도 게시글 제목/본문은 그대로 노출되고, 작성자 닉네임만 익명화된 값으로 바뀐다")
    void authorNicknameReflectsAnonymizedUserAfterWithdrawal() {
        final MbtiMetadata mbti = mbtiOf("INTJ");
        final User author = User.builder()
                .id(UUID.randomUUID())
                .authServerId(UUID.randomUUID())
                .email("original@example.com")
                .nickname("원래닉네임")
                .role(UserRole.USER)
                .state(UserState.NORMAL)
                .mbtiVerified(MbtiVerified.SELF)
                .mbti(mbti)
                .build();
        final Post post = Post.builder()
                .title("탈퇴 전에 쓴 글")
                .content("{}")
                .contentPreview("본문 미리보기")
                .postType(PostType.FREE)
                .user(author)
                .authorMbti(mbti)
                .build();

        final PostSummaryResponse before = PostSummaryResponse.from(post, "https://img.example.com");
        assertThat(before.authorNickname()).isEqualTo("원래닉네임");

        // UserService.performWithdrawal()이 실제로 호출하는 것과 동일한 메소드 — 같은 Post row를 다시 조회하는 상황을 재현
        author.anonymize();
        final PostSummaryResponse after = PostSummaryResponse.from(post, "https://img.example.com");

        assertThat(after.title()).isEqualTo(before.title());
        assertThat(after.contentPreview()).isEqualTo(before.contentPreview());
        assertThat(after.authorNickname())
                .isNotEqualTo("원래닉네임")
                .startsWith("wd_");
    }

    /** MbtiMetadata는 @NoArgsConstructor(PROTECTED) + private id, 빌더/세터가 없어 리플렉션으로 생성 */
    private MbtiMetadata mbtiOf(final String id) {
        try {
            final Constructor<MbtiMetadata> ctor = MbtiMetadata.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            final MbtiMetadata mbti = ctor.newInstance();
            ReflectionTestUtils.setField(mbti, "id", id);
            return mbti;
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
