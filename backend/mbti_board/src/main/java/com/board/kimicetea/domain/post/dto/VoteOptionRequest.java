package com.board.kimicetea.domain.post.dto;

import jakarta.validation.constraints.Size;

/**
 * 투표 선택지 작성 요청 DTO
 * 주의: optionText와 mbtiTarget 중 하나는 반드시 존재해야 함
 * - MBTI 투표: mbtiTarget 설정, optionText null 가능
 * - 자유 투표: optionText 설정, mbtiTarget null
 */
public record VoteOptionRequest(

        @Size(max = 100)
        String optionText,

        // MBTI 투표 선택지일 때 MBTI ID ("INFP" 등), 자유 선택지는 null
        String mbtiTarget
) {
    public boolean hasMbtiTarget() {
        return mbtiTarget != null && !mbtiTarget.isBlank();
    }
}
