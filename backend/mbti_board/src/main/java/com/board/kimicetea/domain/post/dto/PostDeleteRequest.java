package com.board.kimicetea.domain.post.dto;

/**
 * 게시글 삭제 요청 DTO — 비회원은 guestPassword 필수, 회원은 null 허용
 */
public record PostDeleteRequest(String guestPassword) {}
