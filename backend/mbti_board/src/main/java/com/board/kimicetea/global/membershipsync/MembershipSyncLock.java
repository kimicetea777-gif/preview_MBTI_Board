package com.board.kimicetea.global.membershipsync;

import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BooleanSupplier;

/**
 * [역할] 멤버십 폴링(04:15), 확정 배치, 재시도 작업(10초)이 서로 겹치지 않게 하는 프로세스 내 락.
 *        같은 유저를 동시에 만지지 않게 하고, 폴러가 페이지마다 읽는 "미해결 유저 집합"이 처리 중에 바뀌지 않게 한다.
 * [주의사항] 단일 인스턴스 전제다 — 서버를 여러 대로 늘리면 DB 락(예: advisory lock)으로 바꿔야 한다.
 *           가상 스레드에서 블로킹 작업을 synchronized로 감싸면 캐리어 스레드가 고정(pinning)되므로 ReentrantLock을 쓴다.
 *           재진입 가능하다 — 스케줄러가 잡은 채로 sync()가 다시 잡아도 막히지 않는다.
 */
@Component
public class MembershipSyncLock {

    private final ReentrantLock lock = new ReentrantLock();

    /** 락을 기다렸다가 실행하고 결과를 돌려준다 — 하루 한 번 도는 폴링(성공 여부 반환)용 */
    public boolean callLocked(final BooleanSupplier task) {
        lock.lock();
        try {
            return task.getAsBoolean();
        } finally {
            lock.unlock();
        }
    }

    /** 락을 기다렸다가 실행한다 — 폴링 다음 확정 배치까지 한 덩어리로 묶을 때 */
    public void runLocked(final Runnable task) {
        lock.lock();
        try {
            task.run();
        } finally {
            lock.unlock();
        }
    }

    /** 락을 바로 못 잡으면 실행하지 않고 false — 10초마다 도는 재시도 작업용(폴링 중이면 이번 틱은 건너뜀) */
    public boolean tryRunLocked(final Runnable task) {
        if (!lock.tryLock()) {
            return false;
        }
        try {
            task.run();
            return true;
        } finally {
            lock.unlock();
        }
    }
}
