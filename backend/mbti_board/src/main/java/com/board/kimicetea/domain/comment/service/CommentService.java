package com.board.kimicetea.domain.comment.service;

import com.board.kimicetea.domain.comment.dto.CommentCreateRequest;
import com.board.kimicetea.domain.comment.dto.CommentResponse;
import com.board.kimicetea.domain.comment.dto.CommentUpdateRequest;
import com.board.kimicetea.domain.comment.entity.Comment;
import com.board.kimicetea.domain.comment.entity.CommentReaction;
import com.board.kimicetea.domain.comment.repository.CommentReactionRepository;
import com.board.kimicetea.domain.comment.repository.CommentRepository;
import com.board.kimicetea.domain.mbti.MbtiMetadataSupport;
import com.board.kimicetea.domain.mbti.entity.MbtiMetadata;
import com.board.kimicetea.global.common.util.GuestPasswordHelper;
import com.board.kimicetea.global.common.util.RequestUtils;
import com.board.kimicetea.domain.post.entity.Post;
import com.board.kimicetea.domain.post.entity.PostType;
import com.board.kimicetea.domain.post.repository.PostRepository;
import com.board.kimicetea.domain.post.repository.VotePostHistoryRepository;
import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.domain.user.security.CustomUserDetails;
import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 댓글 도메인 비즈니스 로직 (목록 조회, 작성·수정·삭제)
 * 주의: 계층형 구조 조립은 서비스 레이어에서 수행 — DB는 단순 flat 조회
 */
@Service
@RequiredArgsConstructor
public class CommentService {


    private final CommentRepository commentRepository;
    private final CommentReactionRepository reactionRepository;
    private final PostRepository postRepository;
    private final VotePostHistoryRepository votePostHistoryRepository;
    private final UserRepository userRepository;
    private final MbtiMetadataSupport mbtiMetadataSupport;
    private final PasswordEncoder passwordEncoder;

    /**
     * 게시글의 visible 댓글 전체를 계층형으로 반환
     * — 루트 댓글 목록, 각 루트에 replies 포함
     * 주의: 대댓글(depth=2)은 replies 필드가 비어 있음
     */
    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(final Long postId, final UUID userId) {
        final PostType postType = postRepository.findPostTypeById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        final List<Comment> all = commentRepository.findAllVisibleByPostId(postId);
        final Map<Long, Short> reactionMap = buildReactionMap(userId, all);
        final Map<UUID, String> voteOptionMap = buildVoteOptionMap(postId, postType, all);

        // parent_id 기준으로 대댓글 그룹핑
        final Map<Long, List<Comment>> repliesByParentId = all.stream()
                .filter(c -> c.getParent() != null)
                .collect(Collectors.groupingBy(c -> c.getParent().getId()));

        // 루트 댓글만 추려 replies 조립 후 반환
        return all.stream()
                .filter(c -> c.getParent() == null)
                .map(c -> CommentResponse.from(c, repliesByParentId.getOrDefault(c.getId(), List.of()), reactionMap, userId, voteOptionMap))
                .toList();
    }

    /**
     * VOTE 게시글의 댓글 작성자별 투표 선택지 텍스트 맵 — userId → optionText
     * 주의: VOTE 게시글이 아니거나 로그인 댓글이 없으면 빈 맵 반환
     */
    private Map<UUID, String> buildVoteOptionMap(final Long postId, final PostType postType, final List<Comment> comments) {
        if (postType != PostType.VOTE) {
            return Map.of();
        }
        final List<UUID> userIds = comments.stream()
                .map(Comment::getUser)
                .filter(Objects::nonNull)
                .map(User::getId)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return votePostHistoryRepository.findByVotePost_IdAndUser_IdIn(postId, userIds)
                .stream()
                .collect(Collectors.toMap(
                        h -> h.getUser().getId(),
                        h -> h.getVotePostOption().getOptionText(),
                        (existing, replacement) -> existing));
    }

    /**
     * 인증 유저의 댓글 리액션 맵 구성 — commentId → reactionType
     * 주의: 비인증 또는 댓글 없으면 빈 맵 반환
     */
    private Map<Long, Short> buildReactionMap(final UUID userId, final List<Comment> comments) {
        if (userId == null || comments.isEmpty()) {
            return Map.of();
        }
        final List<Long> commentIds = comments.stream().map(Comment::getId).toList();
        return reactionRepository.findByUser_IdAndComment_IdIn(userId, commentIds)
                .stream()
                .collect(Collectors.toMap(r -> r.getComment().getId(), CommentReaction::getReactionType));
    }

    /**
     * 댓글·대댓글 작성
     * 주의:
     * - 대댓글은 1단계까지만 허용 (parent가 이미 대댓글이면 400)
     * - parentId가 다른 게시글 소속이면 COMMENT_NOT_FOUND
     * - DEBATE 게시글은 debateStance(1/-1) 필수, 나머지는 null이어야 함
     */
    @Transactional
    public CommentResponse createComment(final Long postId, final CommentCreateRequest request,
            final CustomUserDetails userDetails, final HttpServletRequest httpRequest) {

        final Post post = findPostOrThrow(postId);

        // 부모 댓글 처리
        final Comment parent = resolveParent(request.parentId(), postId);

        // 회원/비회원 분기
        final User user;
        final MbtiMetadata authorMbti;
        if (userDetails != null) {
            user = findUserOrThrow(userDetails.userId());
            authorMbti = user.getMbti();
        } else {
            GuestPasswordHelper.requirePassword(request.guestPassword());
            GuestPasswordHelper.requireTermsAgreement(
                    request.termsAgreed(), request.privacyAgreed(), request.age14Agreed());
            user = null;
            authorMbti = findNoneMbtiOrThrow();
        }

        // debateStance 검증
        validateDebateStance(post, request.debateStance());

        final Comment comment = Comment.builder()
                .content(request.content().strip())
                .post(post)
                .user(user)
                .authorMbti(authorMbti)
                .parent(parent)
                .debateStance(request.debateStance())
                .guestPassword(GuestPasswordHelper.encode(user, request.guestPassword(), passwordEncoder))
                .authorIp(RequestUtils.extractClientIp(httpRequest))
                .build();

        final UUID requestUserId = userDetails != null ? userDetails.userId() : null;
        return CommentResponse.from(commentRepository.save(comment), requestUserId);
    }

    /**
     * parentId 검증 및 Comment 조회
     * - parentId가 없으면 null 반환 (루트 댓글)
     * - 해당 게시글 소속이 아니거나 삭제된 경우 COMMENT_NOT_FOUND
     * - 이미 대댓글인 경우 COMMENT_REPLY_DEPTH_EXCEEDED
     */
    private Comment resolveParent(final Long parentId, final Long postId) {
        if (parentId == null) {
            return null;
        }

        final Comment parent = commentRepository.findByIdAndIsVisibleTrue(parentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        // 다른 게시글 소속 방지 — FK 프록시 ID 비교로 추가 쿼리 없음
        if (!parent.getPost().getId().equals(postId)) {
            throw new CustomException(ErrorCode.COMMENT_NOT_FOUND);
        }

        // 대댓글의 대댓글 방지
        if (parent.getParent() != null) {
            throw new CustomException(ErrorCode.COMMENT_REPLY_DEPTH_EXCEEDED);
        }

        return parent;
    }

    /**
     * DEBATE 게시글은 stance(1/-1) 필수, 나머지 게시글은 null이어야 함
     */
    private void validateDebateStance(final Post post, final Short debateStance) {
        final boolean isDebate = post.getPostType() == PostType.DEBATE;

        if (isDebate) {
            if (debateStance == null || (debateStance != 1 && debateStance != -1)) {
                throw new CustomException(ErrorCode.DEBATE_STANCE_REQUIRED);
            }
        } else if (debateStance != null) {
            throw new CustomException(ErrorCode.DEBATE_STANCE_NOT_ALLOWED);
        }
    }

    /**
     * 댓글 수정 — content 변경, 소유권 검증 필수
     */
    @Transactional
    public CommentResponse updateComment(final Long postId, final Long commentId,
            final CommentUpdateRequest request, final CustomUserDetails userDetails) {
        final Comment comment = findCommentForPostOrThrow(commentId, postId);
        verifyOwnership(comment, userDetails, request.guestPassword());
        comment.update(request.content().strip());
        final UUID requestUserId = userDetails != null ? userDetails.userId() : null;
        return CommentResponse.from(comment, requestUserId);
    }

    /**
     * 댓글 소프트 삭제 — is_visible = false, 소유권 검증 필수
     */
    @Transactional
    public void deleteComment(final Long postId, final Long commentId,
            final String rawGuestPassword, final CustomUserDetails userDetails) {
        final Comment comment = findCommentForPostOrThrow(commentId, postId);
        verifyOwnership(comment, userDetails, rawGuestPassword);
        comment.hide();
    }

    /**
     * 회원/비회원 소유권 검증
     * - 비회원 댓글: guestPassword BCrypt 검증
     * - 회원 댓글: JWT 유저 UUID 비교
     * - 탈퇴 회원 댓글(user == null, guestPassword == null): 수정·삭제 불가
     */
    private void verifyOwnership(final Comment comment, final CustomUserDetails userDetails,
            final String rawGuestPassword) {
        if (comment.getGuestPassword() != null) {
            GuestPasswordHelper.requirePassword(rawGuestPassword);
            if (!passwordEncoder.matches(rawGuestPassword, comment.getGuestPassword())) {
                throw new CustomException(ErrorCode.GUEST_PASSWORD_MISMATCH);
            }
            return;
        }

        if (comment.getUser() == null || userDetails == null
                || !comment.getUser().getId().equals(userDetails.userId())) {
            throw new CustomException(ErrorCode.COMMENT_FORBIDDEN);
        }
    }

    /**
     * 댓글 단건 조회 + 해당 게시글 소속 검증 (단일 쿼리)
     * 주의: 다른 게시글 댓글 ID로 접근 시 COMMENT_NOT_FOUND 반환 — ID 열거 공격 방지
     */
    private Comment findCommentForPostOrThrow(final Long commentId, final Long postId) {
        return commentRepository.findByIdAndPost_IdAndIsVisibleTrue(commentId, postId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
    }

    private Post findPostOrThrow(final Long postId) {
        return postRepository.findWithDetailsById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
    }

    private User findUserOrThrow(final UUID userId) {
        return userRepository.findWithMbtiById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private MbtiMetadata findNoneMbtiOrThrow() {
        return mbtiMetadataSupport.getNoneMbti();
    }
}
