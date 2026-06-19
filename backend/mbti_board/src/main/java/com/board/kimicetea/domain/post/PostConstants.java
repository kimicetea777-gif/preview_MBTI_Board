package com.board.kimicetea.domain.post;

public final class PostConstants {

    private PostConstants() {}

    /** targetMbti 필터에서 "전체 대상" 을 의미하는 센티넬 값 */
    public static final String TARGET_ALL = "ALL";

    /** post_types 테이블의 코드 값 */
    public static final String TYPE_FREE    = "FREE";
    public static final String TYPE_VOTE    = "VOTE";
    public static final String TYPE_DEBATE  = "DEBATE";

    /** 게시글 본문에 첨부 가능한 최대 이미지 수 */
    public static final int MAX_IMAGE_COUNT = 3;
}
