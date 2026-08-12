package com.board.kimicetea.global.deletionsync;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PollingCursorRepository extends JpaRepository<PollingCursor, String> {
}
