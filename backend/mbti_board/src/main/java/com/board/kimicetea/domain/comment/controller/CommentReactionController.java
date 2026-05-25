package com.board.kimicetea.domain.comment.controller;

import com.board.kimicetea.domain.comment.dto.CommentReactionRequest;
import com.board.kimicetea.domain.comment.dto.CommentReactionResponse;
import com.board.kimicetea.domain.comment.service.CommentReactionService;
import com.board.kimicetea.domain.user.security.CustomUserDetails;
import com.board.kimicetea.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 댓글 리액션 API 컨트롤러 — 회원 전용
 * 주의: SecurityConfig의 anyRequest().authenticated()가 인증을 담보하므로 별도 permitAll 불필요
 */
@Tag(name = "CommentReaction", description = "댓글 리액션 API (회원 전용)")
@RestController
@RequestMapping("/api/comments/{commentId}/reactions")
@RequiredArgsConstructor
public class CommentReactionController {

    private final CommentReactionService reactionService;

    @Operation(
            summary = "댓글 리액션 토글",
            description = "reactionType 1(좋아요) 또는 -1(싫어요)을 전달합니다. "
                    + "같은 타입이면 취소, 다른 타입이면 전환, 없으면 신규 등록합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토글 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 reactionType"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "미인증 사용자"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글 없음")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<CommentReactionResponse>> toggleReaction(
            @PathVariable final Long commentId,
            @Valid @RequestBody final CommentReactionRequest request,
            @AuthenticationPrincipal final CustomUserDetails userDetails) {

        return ResponseEntity.ok(ApiResponse.ok(
                reactionService.toggleReaction(commentId, request.reactionType(), userDetails.userId())));
    }

    @Operation(
            summary = "댓글 리액션 취소",
            description = "현재 유저의 리액션을 삭제합니다. 리액션이 없어도 200을 반환합니다(멱등).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "취소 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "미인증 사용자"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글 없음")
    })
    @DeleteMapping
    public ResponseEntity<ApiResponse<CommentReactionResponse>> cancelReaction(
            @PathVariable final Long commentId,
            @AuthenticationPrincipal final CustomUserDetails userDetails) {

        return ResponseEntity.ok(ApiResponse.ok(
                reactionService.cancelReaction(commentId, userDetails.userId())));
    }
}
