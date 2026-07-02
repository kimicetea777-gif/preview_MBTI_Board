package com.board.kimicetea.domain.notice.service;

import com.board.kimicetea.domain.notice.dto.NoticeResponse;
import com.board.kimicetea.domain.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 공개 공지사항 서비스 — 활성 공지 목록 조회 전용
 * 주의: ADMIN 전용 CRUD는 AdminNoticeService 사용
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;

    /** 현재 시각 기준 노출 가능한 공지 목록 반환 (priority DESC) */
    public List<NoticeResponse> getActiveNotices() {
        return noticeRepository.findActive(Instant.now())
                .stream()
                .map(NoticeResponse::from)
                .toList();
    }
}
