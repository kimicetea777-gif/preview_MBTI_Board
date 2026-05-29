package com.board.kimicetea.global.image.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 이미지 취소 요청 DTO — 게시글 작성 취소 시 프론트에서 업로드된 URL 목록을 전달
 * 주의: PENDING 상태인 레코드만 처리하므로 이미 저장된 게시글 이미지는 영향받지 않음
 */
public record ImageCancelRequest(
        @NotEmpty @Size(max = 20) List<String> urls
) {}
