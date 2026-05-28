package com.board.kimicetea.domain.post.dto;

import com.board.kimicetea.domain.post.entity.DebatePost;
import com.board.kimicetea.domain.post.entity.Post;
import com.board.kimicetea.domain.post.entity.VotePost;
import com.board.kimicetea.domain.post.entity.VotePostHistory;
import com.board.kimicetea.domain.user.enums.MbtiVerified;

import java.time.Instant;
import java.util.UUID;

/**
 * 게시글 상세 응답 DTO — content 포함, isOwner로 수정·삭제 버튼 노출 제어
 * 주의: voteResult는 VOTE 타입, debateResult는 DEBATE 타입일 때만 포함 — 나머지는 null
 */
public record PostDetailResponse(
                Long id,
                String title,
                String content,
                String authorNickname,
                String authorMbtiId,
                String authorMbtiColor,
                String mbtiVerified,
                String mbtiVerifiedColor,
                String targetMbtiId,
                int viewCount,
                int commentCount,
                boolean isOwner,
                boolean isGuest,
                Instant createdAt,
                Instant updatedAt,
                VoteResultResponse voteResult,
                DebateSummaryResponse debateResult) {

        /**
         * FREE 게시글용 팩토리
         * @param currentUserId 현재 요청자 UUID — 비회원이면 null
         */
        public static PostDetailResponse from(final Post post, final UUID currentUserId) {
                return build(post, currentUserId, null, null, null);
        }

        /**
         * VOTE 게시글용 팩토리
         * @param votePost options이 JOIN FETCH된 VotePost
         * @param history  본인 투표 내역 — 비로그인 또는 미투표 시 null
         */
        public static PostDetailResponse from(final Post post, final UUID currentUserId,
                        final VotePost votePost, final VotePostHistory history) {
                return build(post, currentUserId, votePost, history, null);
        }

        /**
         * DEBATE 게시글용 팩토리
         * @param debatePost 토론 메타 (proLabel/conLabel/찬반 카운트/마감일)
         */
        public static PostDetailResponse from(final Post post, final UUID currentUserId,
                        final DebatePost debatePost) {
                return build(post, currentUserId, null, null, debatePost);
        }

        private static PostDetailResponse build(final Post post, final UUID currentUserId,
                        final VotePost votePost, final VotePostHistory history,
                        final DebatePost debatePost) {
                final boolean hasUser = post.getUser() != null;

                final boolean isOwner = hasUser && (currentUserId != null)
                                && post.getUser().getId().equals(currentUserId);

                final VoteResultResponse voteResult = (votePost != null)
                                ? VoteResultResponse.from(votePost, history)
                                : null;

                final DebateSummaryResponse debateResult = (debatePost != null)
                                ? DebateSummaryResponse.from(debatePost)
                                : null;

                return new PostDetailResponse(
                                post.getId(),
                                post.getTitle(),
                                post.getContent(),
                                hasUser ? post.getUser().getNickname() : "(비회원)",
                                post.getAuthorMbti().getId(),
                                post.getAuthorMbti().getColorCode(),
                                hasUser ? post.getUser().getMbtiVerified().name() : MbtiVerified.DEFAULT.name(),
                                hasUser ? post.getUser().getMbtiVerified().getColorCode()
                                                : MbtiVerified.DEFAULT.getColorCode(),
                                post.getTargetMbti() != null ? post.getTargetMbti().getId() : null,
                                post.getViewCount(),
                                post.getCommentCount(),
                                isOwner,
                                post.getGuestPassword() != null,
                                post.getCreatedAt(),
                                post.getUpdatedAt(),
                                voteResult,
                                debateResult);
        }
}
