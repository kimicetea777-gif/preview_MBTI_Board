package com.board.kimicetea.domain.post.dto;

import com.board.kimicetea.domain.post.entity.Post;
import com.board.kimicetea.domain.user.enums.MbtiVerified;

import java.time.Instant;
import java.util.UUID;

/**
 * 게시글 상세 응답 DTO — content 포함, isOwner로 수정·삭제 버튼 노출 제어
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
        int viewCount,
        int commentCount,
        boolean isOwner,
        boolean isGuest,
        Instant createdAt,
        Instant updatedAt
) {
    /**
     * @param currentUserId 현재 요청자 UUID — 비회원이면 null
     */
    public static PostDetailResponse from(final Post post, final UUID currentUserId) {
        final boolean hasUser = post.getUser() != null;

        // 회원 작성 글: 작성자와 현재 유저가 일치할 때 isOwner
        // 비회원 작성 글: isOwner = false (삭제는 guestPassword로 별도 처리)
        final boolean isOwner = hasUser && currentUserId != null
                && post.getUser().getId().equals(currentUserId);

        return new PostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                hasUser ? post.getUser().getNickname() : "(탈퇴한 사용자)",
                post.getAuthorMbti().getId(),
                post.getAuthorMbti().getColorCode(),
                hasUser ? post.getUser().getMbtiVerified().name() : MbtiVerified.DEFAULT.name(),
                hasUser ? post.getUser().getMbtiVerified().getColorCode() : MbtiVerified.DEFAULT.getColorCode(),
                post.getViewCount(),
                post.getCommentCount(),
                isOwner,
                post.getGuestPassword() != null,
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
