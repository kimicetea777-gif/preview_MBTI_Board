package com.board.kimicetea.domain.comment.dto;

import com.board.kimicetea.domain.comment.entity.Comment;
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

class CommentResponseTest {

    @Test
    @DisplayName("작성자가 탈퇴(익명화)해도 댓글 내용은 그대로 노출되고, 작성자 닉네임만 익명화된 값으로 바뀐다")
    void authorNicknameReflectsAnonymizedUserAfterWithdrawal() {
        final MbtiMetadata mbti = mbtiOf("ENFP");
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
                .title("게시글")
                .content("{}")
                .postType(PostType.FREE)
                .authorMbti(mbti)
                .build();
        final Comment comment = Comment.builder()
                .content("탈퇴 전에 쓴 댓글")
                .post(post)
                .user(author)
                .authorMbti(mbti)
                .build();

        final CommentResponse before = CommentResponse.from(comment, author.getId());
        assertThat(before.authorNickname()).isEqualTo("원래닉네임");

        // UserService.performWithdrawal()이 실제로 호출하는 것과 동일한 메소드 — 같은 Comment row를 다시 조회하는 상황을 재현
        author.anonymize();
        final CommentResponse after = CommentResponse.from(comment, author.getId());

        assertThat(after.content()).isEqualTo(before.content());
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
