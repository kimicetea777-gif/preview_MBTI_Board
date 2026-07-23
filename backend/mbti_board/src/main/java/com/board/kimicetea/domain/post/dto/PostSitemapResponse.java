package com.board.kimicetea.domain.post.dto;

import java.time.Instant;

/**
 * sitemap.xml 생성 전용 응답 DTO — id·createdAt만 필요, JOIN 없는 최소 프로젝션
 */
public record PostSitemapResponse(Long id, Instant createdAt) {
}
