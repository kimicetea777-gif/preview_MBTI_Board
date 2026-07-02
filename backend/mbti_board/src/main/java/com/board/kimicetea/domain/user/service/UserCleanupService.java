package com.board.kimicetea.domain.user.service;

import com.board.kimicetea.domain.user.entity.WithdrawalLog;
import com.board.kimicetea.domain.user.enums.UserState;
import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.domain.user.repository.UserRetentionLogRepository;
import com.board.kimicetea.domain.user.repository.WithdrawalLogRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 탈퇴 유저 정기 삭제 · 법적 보존 로그 만료 삭제 스케줄러
 * 주의: PII는 탈퇴 시점에 즉시 익명화됨 — 배치는 빈 껍데기 레코드만 제거
 * 주의: user_retention_logs(email 암호화·IP)는 12개월 보관 후 04:05 배치에서 삭제
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCleanupService {

    private final UserRepository userRepository;
    private final WithdrawalLogRepository withdrawalLogRepository;
    private final UserRetentionLogRepository userRetentionLogRepository;

    /** 매일 오전 04:00 — WITHDRAWN 상태 유저 로그 기록 후 일괄 삭제 */
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void cleanupWithdrawnUsers() {
        final long count = userRepository.countByState(UserState.WITHDRAWN);
        if (count == 0) {
            log.debug("탈퇴 유저 배치 삭제 — 대상 없음");
            return;
        }
        final List<WithdrawalLog> logs = LongStream.range(0, count)
                .mapToObj(ignored -> WithdrawalLog.create())
                .collect(Collectors.toCollection(ArrayList::new));
        withdrawalLogRepository.saveAll(logs);
        userRepository.bulkDeleteByState(UserState.WITHDRAWN);
        log.info("탈퇴 유저 배치 삭제 완료 — {}명", count);
    }

    /** 매일 오전 04:05 — 12개월 만료된 법적 보존 로그 삭제 (전기통신사업법 §83의3) */
    @Scheduled(cron = "0 5 4 * * *")
    @Transactional
    public void cleanupExpiredRetentionLogs() {
        final Instant now = Instant.now();
        final long count = userRetentionLogRepository.countByExpiresAtBefore(now);
        if (count == 0) {
            log.debug("만료된 법적 보존 로그 — 대상 없음");
            return;
        }
        userRetentionLogRepository.bulkDeleteExpiredBefore(now);
        log.info("만료된 법적 보존 로그 삭제 완료 — {}건", count);
    }
}
