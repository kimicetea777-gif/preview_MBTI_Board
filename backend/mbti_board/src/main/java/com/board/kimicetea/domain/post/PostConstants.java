package com.board.kimicetea.domain.post;

public final class PostConstants {

    private PostConstants() {}

    /** targetMbti 필터에서 "전체 대상" 을 의미하는 센티넬 값 */
    public static final String TARGET_ALL = "ALL";

    /** 게시글 본문에 첨부 가능한 최대 이미지 수 */
    public static final int MAX_IMAGE_COUNT = 3;

    /** 투표 선택지 최대 개수 (MBTI 16종 대응) */
    public static final int VOTE_OPTION_MAX = 16;
}
