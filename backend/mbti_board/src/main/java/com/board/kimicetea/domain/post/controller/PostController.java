package com.board.kimicetea.domain.post.controller;

import com.board.kimicetea.domain.post.PostSortType;
import com.board.kimicetea.domain.post.dto.DebatePostCreateRequest;
import com.board.kimicetea.domain.post.dto.DebateStanceRequest;
import com.board.kimicetea.domain.post.dto.PostCreateRequest;
import com.board.kimicetea.domain.post.dto.PostDeleteRequest;
import com.board.kimicetea.domain.post.dto.PostDetailResponse;
import com.board.kimicetea.domain.post.dto.PostSummaryResponse;
import com.board.kimicetea.domain.post.dto.PostUpdateRequest;
import com.board.kimicetea.domain.post.dto.VotePostCreateRequest;
import com.board.kimicetea.domain.post.service.PostService;
import com.board.kimicetea.domain.user.security.CustomUserDetails;
import com.board.kimicetea.global.common.ApiConstants;
import com.board.kimicetea.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 게시글 API 컨트롤러
 * 주의: GET은 인증 불필요, POST·PATCH·DELETE는 회원·비회원 모두 허용 (서비스에서 분기)
 */
@Tag(name = "Post", description = "게시글 API")
@Validated
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

        private final PostService postService;

        @Operation(summary = "게시글 목록 조회", description = "게시판 유형·정렬·MBTI 필터로 게시글 목록을 페이지 단위로 조회합니다. type 생략 시 FREE·VOTE·DEBATE 전체 유형 혼합 반환.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
        })
        @GetMapping
        public ResponseEntity<ApiResponse<Page<PostSummaryResponse>>> getPosts(
                        @RequestParam(required = false) final String type,
                        @RequestParam(defaultValue = "LATEST") final PostSortType sort,
                        @RequestParam(required = false) final String authorMbti,
                        @RequestParam(defaultValue = "ALL") final String targetMbti,
                        @RequestParam(defaultValue = "0") @Min(0) final int page,
                        @RequestParam(defaultValue = "20") @Min(1) @Max(100) final int size) {

                final Page<PostSummaryResponse> result = postService.getPosts(type, sort, authorMbti, targetMbti,
                                PageRequest.of(page, size));
                return ResponseEntity.ok(ApiResponse.ok(result));
        }

        @Operation(summary = "게시글 단건 조회", description = "게시글 ID로 상세 내용을 조회합니다. 조회 시 viewCount가 1 증가합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음")
        })
        @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<PostDetailResponse>> getPost(
                        @PathVariable final Long id,
                        @AuthenticationPrincipal final CustomUserDetails userDetails) {

                final java.util.UUID currentUserId = (userDetails != null) ? userDetails.userId() : null;
                return ResponseEntity.ok(ApiResponse.ok(postService.getPost(id, currentUserId)));
        }

        @Operation(summary = "자유글 작성", description = "회원 또는 비회원(guestPassword 필수)이 자유글(FREE)을 작성합니다. VOTE·DEBATE 타입은 전용 엔드포인트를 사용해야 합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "작성 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 요청")
        })
        @PostMapping
        public ResponseEntity<ApiResponse<PostDetailResponse>> createPost(
                        @Valid @RequestBody final PostCreateRequest request,
                        @AuthenticationPrincipal final CustomUserDetails userDetails,
                        final HttpServletRequest httpRequest) {

                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.ok(postService.createPost(request, userDetails, httpRequest)));
        }

        @Operation(summary = "토론 게시글 작성", description = "회원 또는 비회원(guestPassword 필수)이 토론(DEBATE) 게시글을 작성합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "작성 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 요청")
        })
        @PostMapping("/debate")
        public ResponseEntity<ApiResponse<PostDetailResponse>> createDebatePost(
                        @Valid @RequestBody final DebatePostCreateRequest request,
                        @AuthenticationPrincipal final CustomUserDetails userDetails,
                        final HttpServletRequest httpRequest) {

                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.ok(postService.createDebatePost(request, userDetails, httpRequest)));
        }

        @Operation(summary = "투표 게시글 작성", description = "회원이 투표(VOTE) 게시글을 작성합니다. 선택지는 최소 2개 이상 필요합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "작성 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 요청"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요")
        })
        @PostMapping("/vote")
        public ResponseEntity<ApiResponse<PostDetailResponse>> createVotePost(
                        @Valid @RequestBody final VotePostCreateRequest request,
                        @AuthenticationPrincipal final CustomUserDetails userDetails,
                        final HttpServletRequest httpRequest) {

                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.ok(postService.createVotePost(request, userDetails, httpRequest)));
        }

        @Operation(summary = "토론 진영 투표", description = "회원이 토론 게시글에 찬성(1) 또는 반대(-1) 의견을 선택합니다. 이미 선택한 경우 변경됩니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "투표 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "마감된 토론 또는 잘못된 stance 값"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요")
        })
        @PostMapping("/debate/{id}/stance")
        public ResponseEntity<ApiResponse<Void>> voteDebateStance(
                        @PathVariable final Long id,
                        @Valid @RequestBody final DebateStanceRequest request,
                        @AuthenticationPrincipal final CustomUserDetails userDetails) {

                postService.voteDebateStance(id, request, userDetails);
                return ResponseEntity.ok(ApiResponse.ok(null));
        }

        @Operation(summary = "토론 진영 투표 취소", description = "회원이 자신의 토론 진영 선택을 취소합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "취소 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요")
        })
        @DeleteMapping("/debate/{id}/stance")
        public ResponseEntity<ApiResponse<Void>> removeDebateStance(
                        @PathVariable final Long id,
                        @AuthenticationPrincipal final CustomUserDetails userDetails) {

                postService.removeDebateStance(id, userDetails);
                return ResponseEntity.ok(ApiResponse.ok(null));
        }

        @Operation(summary = "게시글 수정", description = "작성자(회원) 또는 비회원(guestPassword 검증)이 게시글을 수정합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음")
        })
        @PatchMapping("/{id}")
        public ResponseEntity<ApiResponse<PostDetailResponse>> updatePost(
                        @PathVariable final Long id,
                        @Valid @RequestBody final PostUpdateRequest request,
                        @AuthenticationPrincipal final CustomUserDetails userDetails) {

                return ResponseEntity.ok(ApiResponse.ok(postService.updatePost(id, request, userDetails)));
        }

        @Operation(summary = "게시글 삭제", description = "작성자(회원) 또는 비회원(guestPassword 검증)이 게시글을 소프트 삭제합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음")
        })
        @DeleteMapping("/{id}")
        public ResponseEntity<ApiResponse<Void>> deletePost(
                        @PathVariable final Long id,
                        @RequestHeader(value = ApiConstants.HEADER_GUEST_PASSWORD, required = false) final String guestPassword,
                        @AuthenticationPrincipal final CustomUserDetails userDetails) {

                postService.deletePost(id, new PostDeleteRequest(guestPassword), userDetails);
                return ResponseEntity.ok(ApiResponse.ok("게시글이 삭제되었습니다."));
        }
}
