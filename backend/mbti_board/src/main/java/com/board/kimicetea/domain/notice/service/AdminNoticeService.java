package com.board.kimicetea.domain.notice.service;

import com.board.kimicetea.domain.notice.dto.AdminNoticeResponse;
import com.board.kimicetea.domain.notice.dto.NoticeCreateRequest;
import com.board.kimicetea.domain.notice.dto.NoticeUpdateRequest;
import com.board.kimicetea.domain.notice.entity.Notice;
import com.board.kimicetea.domain.notice.repository.NoticeRepository;
import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 어드민 공지사항 CRUD 서비스
 * 주의: SecurityConfig에서 /api/admin/** → ROLE_ADMIN 보호 — 권한 체크 불필요
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminNoticeService {

    private final NoticeRepository noticeRepository;

    /** 전체 공지 목록 조회 (비활성 포함, priority DESC → id DESC) */
    public List<AdminNoticeResponse> getAllNotices() {
        return noticeRepository.findAll(Sort.by(
                        Sort.Order.desc("priority"),
                        Sort.Order.desc("id")))
                .stream()
                .map(AdminNoticeResponse::from)
                .toList();
    }

    /** 공지 생성 */
    @Transactional
    public AdminNoticeResponse createNotice(final NoticeCreateRequest request) {
        final Notice notice = Notice.builder()
                .title(request.title())
                .content(request.content())
                .isActive(request.isActive())
                .priority(request.priority())
                .startsAt(request.startsAt())
                .endsAt(request.endsAt())
                .build();
        return AdminNoticeResponse.from(noticeRepository.save(notice));
    }

    /** 공지 수정 */
    @Transactional
    public AdminNoticeResponse updateNotice(final long id, final NoticeUpdateRequest request) {
        final Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTICE_NOT_FOUND));
        notice.update(request.title(), request.content(), request.isActive(),
                request.priority(), request.startsAt(), request.endsAt());
        return AdminNoticeResponse.from(notice);
    }

    /** 공지 삭제 */
    @Transactional
    public void deleteNotice(final long id) {
        final Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTICE_NOT_FOUND));
        noticeRepository.delete(notice);
    }
}
