package com.board.kimicetea.domain.user.service;

import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.entity.WithdrawalLog;
import com.board.kimicetea.domain.user.enums.UserState;
import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.domain.user.repository.WithdrawalLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 탈퇴 유저 정기 삭제 서비스
 * 주의: PII는 탈퇴 시점에 즉시 익명화됨. 탈퇴 이벤트는 withdrawal_log에 별도 기록되므로
 *       통계 집계와 무관하게 즉시 삭제해도 됨
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCleanupService {

    private final UserRepository userRepository;
    private final WithdrawalLogRepository withdrawalLogRepository;

    /** 매일 오전 04:00 — WITHDRAWN 상태 유저 로그 기록 후 일괄 삭제 */
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void cleanupWithdrawnUsers() {
        final List<User> targets = userRepository.findAllByState(UserState.WITHDRAWN);
        if (targets.isEmpty()) {
            log.debug("탈퇴 유저 배치 삭제 — 대상 없음");
            return;
        }
        final List<WithdrawalLog> logs = targets.stream()
                .map(ignored -> WithdrawalLog.create())
                .toList();
        withdrawalLogRepository.saveAll(logs);
        userRepository.deleteAll(targets);
        log.info("탈퇴 유저 배치 삭제 완료 — {}명", targets.size());
    }
}
