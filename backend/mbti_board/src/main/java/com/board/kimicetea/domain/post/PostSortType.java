package com.board.kimicetea.domain.post;

/** 게시글 목록 정렬 기준 — @RequestParam 변환 시 대문자로 전달 (LATEST·VIEWS·COMMENTS) */
public enum PostSortType {
    LATEST, VIEWS, COMMENTS
}
