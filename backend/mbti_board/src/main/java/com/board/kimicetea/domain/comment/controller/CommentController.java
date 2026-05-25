package com.board.kimicetea.domain.comment.controller;

import com.board.kimicetea.domain.comment.dto.CommentCreateRequest;
import com.board.kimicetea.domain.comment.dto.CommentResponse;
import com.board.kimicetea.domain.comment.dto.CommentUpdateRequest;
import com.board.kimicetea.domain.comment.service.CommentService;
import com.board.kimicetea.domain.user.security.CustomUserDetails;
import com.board.kimicetea.global.common.ApiConstants;
import com.board.kimicetea.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 댓글 API 컨트롤러
 * 주의: GET은 인증 불필요, POST·PATCH·DELETE는 회원·비회원 모두 허용 (서비스에서 분기)
 */
@Tag(name = "Comment", description = "댓글 API")
@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "댓글 목록 조회", description = "게시글의 댓글을 계층형으로 조회합니다. 루트 댓글은 replies 필드에 대댓글을 포함합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(
            @PathVariable final Long postId,
            @AuthenticationPrincipal final CustomUserDetails userDetails) {

        final UUID userId = userDetails != null ? userDetails.userId() : null;
        return ResponseEntity.ok(ApiResponse.ok(commentService.getComments(postId, userId)));
    }

    @Operation(summary = "댓글 작성", description = "회원 또는 비회원(guestPassword 필수)이 댓글을 작성합니다. parentId 포함 시 대댓글로 처리됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 요청 (대댓글 깊이 초과, stance 오류 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 또는 부모 댓글 없음")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @PathVariable final Long postId,
            @Valid @RequestBody final CommentCreateRequest request,
            @AuthenticationPrincipal final CustomUserDetails userDetails) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(commentService.createComment(postId, request, userDetails)));
    }

    @Operation(summary = "댓글 수정", description = "작성자(회원) 또는 비회원(guestPassword 검증)이 댓글 내용을 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글 없음")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable final Long postId,
            @PathVariable final Long id,
            @Valid @RequestBody final CommentUpdateRequest request,
            @AuthenticationPrincipal final CustomUserDetails userDetails) {

        return ResponseEntity.ok(ApiResponse.ok(commentService.updateComment(postId, id, request, userDetails)));
    }

    @Operation(summary = "댓글 삭제", description = "작성자(회원) 또는 비회원(X-Guest-Password 헤더 검증)이 댓글을 소프트 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글 없음")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable final Long postId,
            @PathVariable final Long id,
            @RequestHeader(value = ApiConstants.HEADER_GUEST_PASSWORD, required = false) final String guestPassword,
            @AuthenticationPrincipal final CustomUserDetails userDetails) {

        commentService.deleteComment(postId, id, guestPassword, userDetails);
        return ResponseEntity.ok(ApiResponse.ok("댓글이 삭제되었습니다."));
    }
}
