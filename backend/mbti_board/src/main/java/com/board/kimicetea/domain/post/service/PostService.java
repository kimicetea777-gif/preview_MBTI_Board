package com.board.kimicetea.domain.post.service;

import com.board.kimicetea.domain.mbti.entity.MbtiMetadata;
import com.board.kimicetea.domain.mbti.repository.MbtiMetadataRepository;
import com.board.kimicetea.domain.mbti.MbtiConstants;
import com.board.kimicetea.domain.post.PostConstants;
import com.board.kimicetea.global.common.util.GuestPasswordHelper;
import com.board.kimicetea.domain.post.PostSortType;
import com.board.kimicetea.domain.post.dto.PostCreateRequest;
import com.board.kimicetea.domain.post.dto.PostDeleteRequest;
import com.board.kimicetea.domain.post.dto.PostDetailResponse;
import com.board.kimicetea.domain.post.dto.PostSummaryResponse;
import com.board.kimicetea.domain.post.dto.PostUpdateRequest;
import com.board.kimicetea.domain.post.dto.VoteOptionRequest;
import com.board.kimicetea.domain.post.entity.Post;
import com.board.kimicetea.domain.post.entity.PostType;
import com.board.kimicetea.domain.post.entity.VotePost;
import com.board.kimicetea.domain.post.entity.VotePostHistory;
import com.board.kimicetea.domain.post.entity.VotePostOption;
import com.board.kimicetea.domain.post.repository.PostRepository;
import com.board.kimicetea.domain.post.repository.PostTypeRepository;
import com.board.kimicetea.domain.post.repository.VotePostHistoryRepository;
import com.board.kimicetea.domain.post.repository.VotePostOptionRepository;
import com.board.kimicetea.domain.post.repository.VotePostRepository;
import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.domain.user.security.CustomUserDetails;
import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 게시글 도메인 비즈니스 로직 (목록·단건 조회, 작성·수정·삭제)
 * 주의: 회원/비회원 분기는 userDetails null 여부로 판단
 */
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostTypeRepository postTypeRepository;
    private final MbtiMetadataRepository mbtiMetadataRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VotePostRepository votePostRepository;
    private final VotePostOptionRepository votePostOptionRepository;
    private final VotePostHistoryRepository votePostHistoryRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> getPosts(final String type, final PostSortType sort,
            final String authorMbti, final String targetMbti,
            final Pageable pageable) {
        return postRepository.findPosts(type, sort, authorMbti, targetMbti, pageable)
                .map(PostSummaryResponse::from);
    }

    /**
     * 단건 조회 + viewCount 원자적 증가
     * VOTE 게시글은 options + 본인 투표 내역 함께 반환
     * 주의: @Modifying 쿼리가 포함되므로 readOnly = false
     */
    @Transactional
    public PostDetailResponse getPost(final Long id, final UUID currentUserId) {
        final Post post = findPostOrThrow(id);
        postRepository.increaseViewCount(id);

        if (PostConstants.TYPE_VOTE.equals(post.getPostType().getCode())) {
            final VotePost votePost = votePostRepository.findWithOptionsById(id)
                    .orElseThrow(() -> new CustomException(ErrorCode.VOTE_POST_NOT_FOUND));
            final VotePostHistory history = (currentUserId != null)
                    ? votePostHistoryRepository.findByVotePost_IdAndUser_Id(id, currentUserId).orElse(null)
                    : null;
            return PostDetailResponse.from(post, currentUserId, votePost, history);
        }

        return PostDetailResponse.from(post, currentUserId);
    }

    @Transactional
    public PostDetailResponse createPost(final PostCreateRequest request,
            final CustomUserDetails userDetails) {
        final PostType postType = findPostTypeOrThrow(request.type());

        final User user;
        final MbtiMetadata authorMbti;

        if (userDetails != null) {
            user = findUserOrThrow(userDetails.userId());
            authorMbti = user.getMbti();
        } else {
            // 비회원
            GuestPasswordHelper.requirePassword(request.guestPassword());
            user = null;
            authorMbti = findNoneMbtiOrThrow();
        }

        final MbtiMetadata targetMbti = resolveTargetMbti(request.targetMbti());

        final Post post = Post.builder()
                .title(request.title().strip())
                .content(request.content())
                .guestPassword(GuestPasswordHelper.encode(user, request.guestPassword(), passwordEncoder))
                .user(user)
                .authorMbti(authorMbti)
                .targetMbti(targetMbti)
                .postType(postType)
                .build();

        final Post saved = postRepository.save(post);

        // VOTE 타입이면 VotePost + 선택지 생성
        if (PostConstants.TYPE_VOTE.equals(request.type())) {
            final VotePost votePost = createVotePost(saved, request);
            return PostDetailResponse.from(saved, (userDetails != null) ? userDetails.userId() : null, votePost, null);
        }

        return PostDetailResponse.from(saved, (userDetails != null) ? userDetails.userId() : null);
    }

    /**
     * VotePost + VotePostOption 생성 (트랜잭션 내 호출)
     * 주의:
     * - voteOptions 최소 2개 필수
     * - MBTI 투표 시 같은 mbtiTarget 중복 불가 (DB 인덱스 + 서비스 사전 검증)
     */
    private VotePost createVotePost(final Post post, final PostCreateRequest request) {
        final List<VoteOptionRequest> optionRequests = request.voteOptions();
        if (optionRequests == null || optionRequests.size() < 2) {
            throw new CustomException(ErrorCode.VOTE_OPTIONS_REQUIRED);
        }

        // endsAt이 과거 시각이면 즉시 마감 투표가 생성되므로 차단
        if (request.endsAt() != null && request.endsAt().isBefore(Instant.now())) {
            throw new CustomException(ErrorCode.VOTE_ENDS_AT_INVALID);
        }

        // DB flush 전에는 이전 저장 옵션을 existsByVotePost_Id... 쿼리가 인식 못 하므로
        // 요청 리스트 내 MBTI 중복을 인메모리에서 먼저 검증
        // 또한 MBTI 투표(mbtiTarget 있음)와 자유 투표(null)가 혼합되지 않도록 검증
        final boolean hasMbti = optionRequests.stream()
                .anyMatch(o -> o.mbtiTarget() != null && !o.mbtiTarget().isBlank());
        final boolean hasNonMbti = optionRequests.stream()
                .anyMatch(o -> o.mbtiTarget() == null || o.mbtiTarget().isBlank());
        if (hasMbti && hasNonMbti) {
            throw new CustomException(ErrorCode.VOTE_MBTI_MODE_MIXED);
        }

        final Set<String> seenMbti = new HashSet<>();
        for (final VoteOptionRequest optReq : optionRequests) {
            if (optReq.mbtiTarget() != null && !optReq.mbtiTarget().isBlank()) {
                if (!seenMbti.add(optReq.mbtiTarget())) {
                    throw new CustomException(ErrorCode.VOTE_MBTI_DUPLICATE);
                }
            }
        }

        final VotePost votePost = VotePost.builder()
                .post(post)
                .endsAt(request.endsAt())
                .build();
        votePostRepository.save(votePost);

        for (int i = 0; i < optionRequests.size(); i++) {
            final VoteOptionRequest optReq = optionRequests.get(i);
            MbtiMetadata mbtiTarget = null;

            if (optReq.mbtiTarget() != null && !optReq.mbtiTarget().isBlank()) {
                mbtiTarget = mbtiMetadataRepository.findById(optReq.mbtiTarget())
                        .orElseThrow(() -> new CustomException(ErrorCode.MBTI_NOT_FOUND));
            }

            final VotePostOption option = VotePostOption.builder()
                    .votePost(votePost)
                    .optionText(optReq.optionText())
                    .displayOrder((short) i)
                    .mbtiTarget(mbtiTarget)
                    .build();
            votePostOptionRepository.save(option);
        }

        // votePost가 1차 캐시에 options=[]로 올라가 있어 JOIN FETCH가 캐시를 우선함
        // detach 후 재조회해야 DB에서 options를 올바르게 가져옴
        entityManager.flush();
        entityManager.detach(votePost);
        return votePostRepository.findWithOptionsById(post.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.VOTE_POST_NOT_FOUND));
    }

    @Transactional
    public PostDetailResponse updatePost(final Long id, final PostUpdateRequest request,
            final CustomUserDetails userDetails) {
        final Post post = findPostOrThrow(id);
        verifyOwnership(post, userDetails, request.guestPassword());
        post.update(request.title().strip(), request.content());
        return PostDetailResponse.from(post, userDetails != null ? userDetails.userId() : null);
    }

    @Transactional
    public void deletePost(final Long id, final PostDeleteRequest request,
            final CustomUserDetails userDetails) {
        final Post post = findPostOrThrow(id);
        verifyOwnership(post, userDetails, request.guestPassword());
        post.hide();
    }

    /**
     * 회원/비회원 소유권 검증
     * - 회원 작성 글: JWT 유저와 작성자 UUID 비교
     * - 비회원 작성 글: guestPassword BCrypt 검증
     * - 탈퇴 회원 작성 글(user == null, guestPassword == null): 수정·삭제 불가
     */
    private void verifyOwnership(final Post post, final CustomUserDetails userDetails,
            final String rawGuestPassword) {
        final boolean isGuestPost = (post.getGuestPassword() != null);

        if (isGuestPost) {
            GuestPasswordHelper.requirePassword(rawGuestPassword);
            if (!passwordEncoder.matches(rawGuestPassword, post.getGuestPassword())) {
                throw new CustomException(ErrorCode.GUEST_PASSWORD_MISMATCH);
            }
            return;
        }

        if (post.getUser() == null || userDetails == null
                || !post.getUser().getId().equals(userDetails.userId())) {
            throw new CustomException(ErrorCode.POST_FORBIDDEN);
        }
    }

    private Post findPostOrThrow(final Long id) {
        return postRepository.findWithDetailsById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
    }

    private PostType findPostTypeOrThrow(final String code) {
        return postTypeRepository.findByCode(code)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_TYPE_NOT_FOUND));
    }

    private User findUserOrThrow(final UUID userId) {
        return userRepository.findWithMbtiById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private MbtiMetadata findNoneMbtiOrThrow() {
        return mbtiMetadataRepository.findById(MbtiConstants.NONE_MBTI_ID)
                .orElseThrow(() -> new CustomException(ErrorCode.MBTI_NOT_FOUND));
    }

    /** null / "ALL" 이면 타겟 없음(null 반환), 특정 MBTI ID면 조회 후 반환 */
    private MbtiMetadata resolveTargetMbti(final String targetMbtiId) {
        if (targetMbtiId == null || targetMbtiId.isBlank() || PostConstants.TARGET_ALL.equals(targetMbtiId)) {
            return null;
        }
        return mbtiMetadataRepository.findById(targetMbtiId)
                .orElseThrow(() -> new CustomException(ErrorCode.MBTI_NOT_FOUND));
    }
}
