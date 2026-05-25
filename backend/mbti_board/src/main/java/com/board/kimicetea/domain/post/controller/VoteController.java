package com.board.kimicetea.domain.post.controller;

import com.board.kimicetea.domain.post.dto.VoteRequest;
import com.board.kimicetea.domain.post.dto.VoteResultResponse;
import com.board.kimicetea.domain.post.service.VoteService;
import com.board.kimicetea.domain.user.security.CustomUserDetails;
import com.board.kimicetea.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 투표 API 컨트롤러
 * 주의: castVote는 회원 전용 — SecurityConfig에서 POST /vote 엔드포인트를 authenticated()로 설정
 * 필요
 */
@Tag(name = "Vote", description = "투표 API")
@RestController
@RequestMapping("/api/posts/{postId}/vote")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    @Operation(summary = "투표 참여", description = "투표 선택지를 선택합니다. 이미 투표한 경우 선택지를 변경합니다. 회원 전용.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "투표 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "마감된 투표 또는 잘못된 선택지"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "비로그인"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 또는 선택지 없음")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<VoteResultResponse>> castVote(
            @PathVariable final Long postId,
            @Valid @RequestBody final VoteRequest request,
            @AuthenticationPrincipal final CustomUserDetails userDetails) {

        final UUID currentUserId = (userDetails != null) ? userDetails.userId() : null;
        return ResponseEntity.ok(ApiResponse.ok(voteService.castVote(postId, request, currentUserId)));
    }

    @Operation(summary = "투표 결과 조회", description = "선택지별 득표수와 본인 선택 옵션을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    @GetMapping("/result")
    public ResponseEntity<ApiResponse<VoteResultResponse>> getVoteResult(
            @PathVariable final Long postId,
            @AuthenticationPrincipal final CustomUserDetails userDetails) {

        final UUID currentUserId = (userDetails != null) ? userDetails.userId() : null;
        return ResponseEntity.ok(ApiResponse.ok(voteService.getVoteResult(postId, currentUserId)));
    }
}
