package com.board.kimicetea.global.image.entity;

/** deleted_images 레코드 생명주기 상태 */
public enum DeletedImageStatus {
    /** 업로드됐으나 게시글 저장 미완료 — 24h 후 스케줄러가 삭제 */
    PENDING,
    /** 취소 요청 시 즉시 삭제 실패 — 스케줄러 재시도 대상 */
    DELETE_REQUESTED
}
