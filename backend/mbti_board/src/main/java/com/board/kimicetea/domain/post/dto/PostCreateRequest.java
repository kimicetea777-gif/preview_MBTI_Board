package com.board.kimicetea.domain.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 자유글 게시글 작성 요청 DTO (POST /api/posts)
 * 주의:
 * - guestPassword·termsAgreed·privacyAgreed·age14Agreed는 비회원 전용 — 서비스에서 인증 여부에 따라 필수 검증
 * - VOTE는 POST /api/posts/vote, DEBATE는 POST /api/posts/debate 전용 엔드포인트 사용
 */
public record PostCreateRequest(

                @NotBlank @Size(max = 150) String title,

                @NotBlank @Size(max = 50_000) String content,

                @NotBlank @Size(max = 10) String type,

                // BCrypt 72바이트 한계로 최대 72자 제한
                @Size(min = 4, max = 72) String guestPassword,

                // null 또는 "ALL" 이면 전체 대상 — 특정 MBTI ID("INFP" 등) 지정 시 해당 MBTI 타겟
                String targetMbti,

                /** primitive boolean — 필드 누락 시 false로 역직렬화되어 안전하게 거부됨 (비회원 전용) */
                boolean termsAgreed,
                boolean privacyAgreed,
                boolean age14Agreed) {
}
