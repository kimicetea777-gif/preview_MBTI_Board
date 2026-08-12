package com.board.kimicetea.global.deletionsync;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 매일 오전 04:10 — UserCleanupService의 04:00/04:05 배치 직후, 하루 1회(memo.md "최소 하루 1회" 권장) */
@Component
@RequiredArgsConstructor
public class DeletedUserSyncScheduler {

    private final DeletedUserSyncService deletedUserSyncService;

    @Scheduled(cron = "0 10 4 * * *")
    public void syncDeletedUsers() {
        deletedUserSyncService.sync();
    }
}
