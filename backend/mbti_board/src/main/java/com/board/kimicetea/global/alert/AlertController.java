package com.board.kimicetea.global.alert;

import com.board.kimicetea.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 이메일 에러 알림 제어 컨트롤러 — ADMIN 전용 (/api/admin/**)
 * 주의: mute 상태는 서버 재시작 시 초기화됨 (인메모리 플래그)
 */
@Tag(name = "Admin - Alert", description = "이메일 에러 알림 제어 API")
@RestController
@RequestMapping("/api/admin/alerts")
public class AlertController {

    @Operation(summary = "알림 음소거", description = "이메일 에러 알림을 끕니다. 서버 재시작 시 자동으로 복원됩니다.")
    @PostMapping("/mute")
    public ResponseEntity<ApiResponse<Void>> mute() {
        EmailAlertFlag.mute();
        return ResponseEntity.ok(ApiResponse.ok("이메일 에러 알림이 음소거되었습니다."));
    }

    @Operation(summary = "알림 복원", description = "음소거된 이메일 에러 알림을 다시 활성화합니다.")
    @PostMapping("/unmute")
    public ResponseEntity<ApiResponse<Void>> unmute() {
        EmailAlertFlag.unmute();
        return ResponseEntity.ok(ApiResponse.ok("이메일 에러 알림이 활성화되었습니다."));
    }

    @Operation(summary = "알림 상태 조회", description = "현재 이메일 에러 알림 활성화 여부를 반환합니다.")
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Boolean>> status() {
        return ResponseEntity.ok(ApiResponse.ok(EmailAlertFlag.isEnabled()));
    }
}
