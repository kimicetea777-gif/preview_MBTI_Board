package com.board.kimicetea.domain.post.enums;

/**
 * 게시글 이미지의 Image Server 동기화 상태 — post_images.status (V14 CHECK 제약과 같은 값)
 * 주의: release에는 "포기" 상태가 없다. 대상은 이미 글에서 빠진 이미지라 늦어져도 게시글에 영향이 없어서
 *       성공할 때까지 재시도한다. confirm은 Image Server GC(24시간) 한도가 있어 CONFIRM_FAILED로 결론을 낸다.
 */
public enum PostImageStatus {

    /** 글에 붙었고 아직 확정 전 — 워커가 confirm */
    PENDING_CONFIRM,

    /** 확정 완료 */
    CONFIRMED,

    /** 확정 불가로 결론(이미 만료·삭제됐거나 재시도 소진) — 이미지는 깨진 상태, 사용자가 다시 올려야 함 */
    CONFIRM_FAILED,

    /** 글에서 빠졌거나 글이 삭제됨 — 워커가 release 후 행 삭제 */
    PENDING_RELEASE;

    /** 글에 붙어 있는 상태인가 (떼어 내는 중이 아님) */
    public boolean isAttached() {
        return this != PENDING_RELEASE;
    }
}
