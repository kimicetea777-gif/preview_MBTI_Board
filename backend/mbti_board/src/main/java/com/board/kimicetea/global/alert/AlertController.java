package com.board.kimicetea.global.alert;

import com.board.kimicetea.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 이메일 에러 알림 제어 컨트롤러 — ADMIN 전용 (/api/admin/**)
 * 주의: mute 상태는 서버 재시작 시 초기화됨 (인메모리 플래그)
 */
@Tag(name = "Admin - Alert", description = "이메일 에러 알림 제어 API")
@RestController
@RequestMapping("/api/admin/alerts")
public class AlertController {

    @Operation(summary = "알림 음소거", description = "지정한 AlertType의 이메일 알림을 끕니다. 서버 재시작 시 자동 복원됩니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "음소거 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 AlertType"),
    })
    @PostMapping("/mute/{type}")
    public ResponseEntity<ApiResponse<Void>> mute(@PathVariable final AlertType type) {
        AlertFlagRegistry.mute(type);
        return ResponseEntity.ok(ApiResponse.ok(type + " 알림이 음소거되었습니다."));
    }

    @Operation(summary = "알림 복원", description = "음소거된 AlertType의 이메일 알림을 다시 활성화합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "활성화 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 AlertType"),
    })
    @PostMapping("/unmute/{type}")
    public ResponseEntity<ApiResponse<Void>> unmute(@PathVariable final AlertType type) {
        AlertFlagRegistry.unmute(type);
        return ResponseEntity.ok(ApiResponse.ok(type + " 알림이 활성화되었습니다."));
    }

    @Operation(summary = "알림 상태 조회", description = "전체 AlertType별 이메일 알림 활성화 여부를 반환합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상태 조회 성공"),
    })
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<AlertType, Boolean>>> status() {
        return ResponseEntity.ok(ApiResponse.ok(AlertFlagRegistry.getStatus()));
    }
}
