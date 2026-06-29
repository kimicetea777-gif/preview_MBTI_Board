package com.board.kimicetea.global.image;

import java.util.Set;

public final class ImageConstants {

    private ImageConstants() {}

    /** 게시글당 첨부 이미지 최대 개수 */
    public static final int IMAGE_MAX_SIZE = 3;

    /** S3/R2 deleteObjects API 단일 요청 최대 객체 수 */
    public static final int DELETE_OBJECTS_BATCH_SIZE = 1000;

    /** IP당 이미지 업로드 Rate Limit 최대 횟수 */
    public static final int RATE_LIMIT_MAX = 10;

    /** Rate Limit 윈도우 (초) */
    public static final int RATE_LIMIT_WINDOW_SECONDS = 60;

    /** 허용된 이미지 MIME 타입 목록 */
    public static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/bmp"
    );

    /** 이미지 업로드 최대 파일 크기 (10MB) */
    public static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
}
