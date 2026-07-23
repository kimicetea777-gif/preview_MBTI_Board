package com.board.kimicetea.domain.post.service;

import com.board.kimicetea.domain.mbti.MbtiMetadataSupport;
import com.board.kimicetea.domain.mbti.entity.MbtiMetadata;
import com.board.kimicetea.domain.mbti.repository.MbtiMetadataRepository;
import com.board.kimicetea.domain.post.PostConstants;
import com.board.kimicetea.global.common.util.GuestPasswordHelper;
import com.board.kimicetea.global.common.util.RequestUtils;
import com.board.kimicetea.global.common.util.TiptapContentParser;
import com.board.kimicetea.global.image.service.ImageCleanupService;
import com.board.kimicetea.domain.post.PostSortType;
import com.board.kimicetea.domain.post.dto.DebatePostCreateRequest;
import com.board.kimicetea.domain.post.dto.DebateStanceRequest;
import com.board.kimicetea.domain.post.dto.PostCreateRequest;
import com.board.kimicetea.domain.post.dto.PostDeleteRequest;
import com.board.kimicetea.domain.post.dto.PostDetailResponse;
import com.board.kimicetea.domain.post.dto.MyPostPreviewResponse;
import com.board.kimicetea.domain.post.dto.PostMetaResponse;
import com.board.kimicetea.domain.post.dto.PostSitemapResponse;
import com.board.kimicetea.domain.post.dto.PostSummaryResponse;
import com.board.kimicetea.domain.post.dto.PostUpdateRequest;
import com.board.kimicetea.domain.post.dto.VoteOptionRequest;
import com.board.kimicetea.domain.post.dto.VotePostCreateRequest;
import com.board.kimicetea.domain.post.entity.DebatePost;
import com.board.kimicetea.domain.post.entity.DebateStanceHistory;
import com.board.kimicetea.domain.post.entity.Post;
import com.board.kimicetea.domain.post.entity.PostType;
import com.board.kimicetea.domain.post.entity.VotePost;

import com.board.kimicetea.domain.post.entity.VotePostHistory;
import com.board.kimicetea.domain.post.entity.VotePostOption;
import com.board.kimicetea.domain.post.repository.DebatePostRepository;
import com.board.kimicetea.domain.post.repository.DebateStanceHistoryRepository;
import com.board.kimicetea.domain.post.repository.PostRepository;
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
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 게시글 도메인 비즈니스 로직 (목록·단건 조회, 작성·수정·삭제)
 * 주의: 회원/비회원 분기는 userDetails null 여부로 판단
 */
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final MbtiMetadataRepository mbtiMetadataRepository;
    private final MbtiMetadataSupport mbtiMetadataSupport;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VotePostRepository votePostRepository;
    private final VotePostOptionRepository votePostOptionRepository;
    private final VotePostHistoryRepository votePostHistoryRepository;
    private final DebatePostRepository debatePostRepository;
    private final DebateStanceHistoryRepository debateStanceHistoryRepository;
    private final ImageCleanupService imageCleanupService;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${r2.public-url}")
    private String r2PublicUrl;

    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> getPosts(final String type, final PostSortType sort,
            final String authorMbti, final String targetMbti,
            final Pageable pageable) {
        final String normalizedType = resolveTypeCode(type);
        return postRepository.findPosts(normalizedType, sort, authorMbti, targetMbti, pageable)
                .map(post -> PostSummaryResponse.from(post, r2PublicUrl));
    }

    /** 내 게시글 목록 — typeCode null 이면 전체 유형 반환 */
    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> getMyPosts(final UUID userId, final String typeCode,
            final PostSortType sort, final Pageable pageable) {
        final String normalizedType = resolveTypeCode(typeCode);
        return postRepository.findMyPosts(userId, normalizedType, sort, pageable)
                .map(post -> PostSummaryResponse.from(post, r2PublicUrl));
    }

    /** 마이페이지 내 게시글 프리뷰 — 최근 5개 */
    @Transactional(readOnly = true)
    public List<MyPostPreviewResponse> getMyPostsPreview(final UUID userId) {
        return postRepository.findMyPostsPreview(userId).stream()
                .map(MyPostPreviewResponse::from)
                .toList();
    }

    /**
     * type 파라미터를 대문자로 정규화하고 유효성 검증
     * null·공백이면 null 반환(전체 조회), 유효하지 않은 값이면 400
     */
    private String resolveTypeCode(final String typeCode) {
        if (typeCode == null || typeCode.isBlank()) {
            return null;
        }
        return PostType.from(typeCode).name();
    }

    /**
     * 단건 조회 + viewCount 원자적 증가
     * VOTE: options + 본인 투표 내역 함께 반환
     * DEBATE: proLabel/conLabel/찬반 카운트/마감일 함께 반환
     * 주의: @Modifying 쿼리가 포함되므로 readOnly = false
     */
    @Transactional
    public PostDetailResponse getPost(final Long id, final UUID currentUserId) {
        final Post post = findPostOrThrow(id);
        postRepository.increaseViewCount(id);
        return buildDetailResponse(post, currentUserId);
    }

    /**
     * sitemap.xml 생성 전용 — id·createdAt만 필요해 authorMbti/votePost/debatePost JOIN을 전부 생략
     */
    @Transactional(readOnly = true)
    public Page<PostSitemapResponse> getPostsForSitemap(final String type, final Pageable pageable) {
        return postRepository.findSitemapEntries(PostType.from(type), pageable);
    }

    /**
     * SEO/OG 메타데이터 전용 조회 — viewCount 증가 없음, 인증 무관
     * 주의: title/contentPreview/imageUrls만 필요해 findWithDetailsById의 JOIN을 타지 않음
     */
    @Transactional(readOnly = true)
    public PostMetaResponse getPostMeta(final Long id) {
        final Post post = postRepository.findVisibleById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        return PostMetaResponse.from(post, r2PublicUrl);
    }

    /**
     * Post 엔티티 → PostDetailResponse 변환 (VOTE/DEBATE 부가 정보 포함)
     * 주의: viewCount 증가를 포함하지 않음 — 어드민 모더레이션 조회(AdminPostService)에서도 재사용하기 위해 분리
     */
    public PostDetailResponse buildDetailResponse(final Post post, final UUID currentUserId) {
        final Long id = post.getId();

        if (post.getPostType() == PostType.VOTE) {
            final VotePost votePost = votePostRepository.findWithOptionsById(id)
                    .orElseThrow(() -> new CustomException(ErrorCode.VOTE_POST_NOT_FOUND));
            final VotePostHistory history = (currentUserId != null)
                    ? votePostHistoryRepository.findByVotePost_IdAndUser_Id(id, currentUserId).orElse(null)
                    : null;
            return PostDetailResponse.from(post, currentUserId, votePost, history)
                    .resolveImageUrls(r2PublicUrl);
        }

        if (post.getPostType() == PostType.DEBATE) {
            final DebatePost debatePost = debatePostRepository.findById(id)
                    .orElseThrow(() -> new CustomException(ErrorCode.DEBATE_POST_NOT_FOUND));
            final Short myStance = (currentUserId != null)
                    ? debateStanceHistoryRepository.findStanceByUserIdAndPostId(currentUserId, id).orElse(null)
                    : null;
            return PostDetailResponse.from(post, currentUserId, debatePost, myStance)
                    .resolveImageUrls(r2PublicUrl);
        }

        return PostDetailResponse.from(post, currentUserId).resolveImageUrls(r2PublicUrl);
    }

    /**
     * POST /api/posts/debate/{id}/stance — 진영 투표 (회원 전용)
     * 주의: 동일 진영 재선택 시 무시, 다른 진영 선택 시 업데이트 (트리거가 카운트 보정)
     */
    @Transactional
    public void voteDebateStance(final Long postId, final DebateStanceRequest request,
            final CustomUserDetails userDetails) {
        if (request.stance() != 1 && request.stance() != -1) {
            throw new CustomException(ErrorCode.DEBATE_STANCE_INVALID);
        }

        final DebatePost debatePost = debatePostRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.DEBATE_POST_NOT_FOUND));

        if (debatePost.getEndsAt() != null && debatePost.getEndsAt().isBefore(Instant.now())) {
            throw new CustomException(ErrorCode.DEBATE_STANCE_CLOSED);
        }

        // JWT 인증 완료 → DB 조회 없이 프록시 참조만으로 FK 연관관계 설정
        final User userRef = entityManager.getReference(User.class, userDetails.userId());

        debateStanceHistoryRepository.findByIdUserIdAndIdPostId(userDetails.userId(), postId)
                .ifPresentOrElse(
                        history -> history.changeStance(request.stance()),
                        () -> debateStanceHistoryRepository.save(
                                DebateStanceHistory.create(userRef, debatePost, request.stance())));
    }

    /**
     * DELETE /api/posts/debate/{id}/stance — 진영 투표 취소 (회원 전용)
     */
    @Transactional
    public void removeDebateStance(final Long postId, final CustomUserDetails userDetails) {
        debateStanceHistoryRepository.deleteByIdUserIdAndIdPostId(userDetails.userId(), postId);
    }

    @Transactional
    public PostDetailResponse createPost(final PostCreateRequest request,
            final CustomUserDetails userDetails, final HttpServletRequest httpRequest) {
        // VOTE·DEBATE는 전용 엔드포인트를 사용해야 함
        final PostType postType = PostType.from(request.type());
        if (postType == PostType.VOTE || postType == PostType.DEBATE) {
            throw new CustomException(ErrorCode.POST_TYPE_NOT_ALLOWED);
        }

        final User user;
        final MbtiMetadata authorMbti;

        if (userDetails != null) {
            user = findUserOrThrow(userDetails.userId());
            authorMbti = user.getMbti();
        } else {
            // 비회원
            GuestPasswordHelper.requirePassword(request.guestPassword());
            GuestPasswordHelper.requireTermsAgreement(
                    request.termsAgreed(), request.privacyAgreed(), request.age14Agreed());
            user = null;
            authorMbti = mbtiMetadataSupport.getNoneMbti();
        }

        final MbtiMetadata targetMbti = resolveTargetMbti(request.targetMbti());
        final Set<String> realUrls = TiptapContentParser.extractImageUrls(request.content(), r2PublicUrl);
        final ProcessedContent pc = processContent(request.content());

        final Post post = Post.builder()
                .title(request.title().strip())
                .content(pc.normalized())
                .guestPassword(GuestPasswordHelper.encode(user, request.guestPassword(), passwordEncoder))
                .authorIp(RequestUtils.extractClientIp(httpRequest))
                .user(user)
                .authorMbti(authorMbti)
                .targetMbti(targetMbti)
                .postType(postType)
                .contentPreview(pc.preview())
                .imageUrls(pc.imageKeys())
                .build();

        final Post saved = postRepository.save(post);
        imageCleanupService.commitImages(realUrls);
        return PostDetailResponse.from(saved, (userDetails != null) ? userDetails.userId() : null)
                .resolveImageUrls(r2PublicUrl);
    }

    /**
     * POST /api/posts/debate — 토론 게시글 작성 (회원 전용)
     * 주의: SecurityConfig에서 인증 필수 — userDetails null 도달 불가
     */
    @Transactional
    public PostDetailResponse createDebatePost(final DebatePostCreateRequest request,
            final CustomUserDetails userDetails, final HttpServletRequest httpRequest) {
        final User user = findUserOrThrow(userDetails.userId());
        final MbtiMetadata authorMbti = user.getMbti();
        final MbtiMetadata targetMbti = resolveTargetMbti(request.targetMbti());
        final Set<String> realUrls = TiptapContentParser.extractImageUrls(request.content(), r2PublicUrl);
        final ProcessedContent pc = processContent(request.content());

        final Post post = Post.builder()
                .title(request.title().strip())
                .content(pc.normalized())
                .authorIp(RequestUtils.extractClientIp(httpRequest))
                .user(user)
                .authorMbti(authorMbti)
                .targetMbti(targetMbti)
                .postType(PostType.DEBATE)
                .contentPreview(pc.preview())
                .imageUrls(pc.imageKeys())
                .build();

        final Post saved = postRepository.save(post);
        imageCleanupService.commitImages(realUrls);
        final DebatePost debatePost = createDebatePostMeta(saved, request);
        return PostDetailResponse.from(saved, userDetails.userId(), debatePost)
                .resolveImageUrls(r2PublicUrl);
    }

    /**
     * POST /api/posts/vote — 투표 게시글 작성 (회원 전용)
     * 주의: SecurityConfig에서 인증 필수 — userDetails null 도달 불가
     */
    @Transactional
    public PostDetailResponse createVotePost(final VotePostCreateRequest request,
            final CustomUserDetails userDetails, final HttpServletRequest httpRequest) {
        final User user = findUserOrThrow(userDetails.userId());
        final MbtiMetadata targetMbti = resolveTargetMbti(request.targetMbti());
        final Set<String> realUrls = TiptapContentParser.extractImageUrls(request.content(), r2PublicUrl);
        final ProcessedContent pc = processContent(request.content());

        final Post post = Post.builder()
                .title(request.title().strip())
                .content(pc.normalized())
                .authorIp(RequestUtils.extractClientIp(httpRequest))
                .user(user)
                .authorMbti(user.getMbti())
                .targetMbti(targetMbti)
                .postType(PostType.VOTE)
                .contentPreview(pc.preview())
                .imageUrls(pc.imageKeys())
                .build();

        final Post saved = postRepository.save(post);
        imageCleanupService.commitImages(realUrls);
        final VotePost votePost = createVotePostMeta(saved, request);
        return PostDetailResponse.from(saved, userDetails.userId(), votePost, null)
                .resolveImageUrls(r2PublicUrl);
    }

    /**
     * VotePost + VotePostOption 생성 (트랜잭션 내 호출)
     * 주의:
     * - voteOptions 최소 2개 필수
     * - MBTI 투표 시 같은 mbtiTarget 중복 불가 (DB 인덱스 + 서비스 사전 검증)
     */
    private VotePost createVotePostMeta(final Post post, final VotePostCreateRequest request) {
        final List<VoteOptionRequest> optionRequests = request.voteOptions();
        if (optionRequests == null || optionRequests.size() < 2) {
            throw new CustomException(ErrorCode.VOTE_OPTIONS_REQUIRED);
        }
        if (optionRequests.size() > PostConstants.VOTE_OPTION_MAX) {
            throw new CustomException(ErrorCode.VOTE_OPTIONS_EXCEEDED);
        }

        // endsAt이 과거 시각이면 즉시 마감 투표가 생성되므로 차단
        if (request.endsAt() != null && request.endsAt().isBefore(Instant.now())) {
            throw new CustomException(ErrorCode.VOTE_ENDS_AT_INVALID);
        }

        // DB flush 전에는 이전 저장 옵션을 existsByVotePost_Id... 쿼리가 인식 못 하므로
        // 요청 리스트 내 MBTI 중복을 인메모리에서 먼저 검증
        // 또한 MBTI 투표(mbtiTarget 있음)와 자유 투표(null)가 혼합되지 않도록 검증
        final boolean hasMbti = optionRequests.stream().anyMatch(VoteOptionRequest::hasMbtiTarget);
        final boolean hasNonMbti = optionRequests.stream().anyMatch(o -> !o.hasMbtiTarget());
        if (hasMbti && hasNonMbti) {
            throw new CustomException(ErrorCode.VOTE_MBTI_MODE_MIXED);
        }

        final Set<String> seenMbti = new HashSet<>();
        if (hasMbti) {
            for (final VoteOptionRequest opt : optionRequests) {
                if (!seenMbti.add(opt.mbtiTarget())) {
                    throw new CustomException(ErrorCode.VOTE_MBTI_DUPLICATE);
                }
            }
        } else {
            final Set<String> seenText = new HashSet<>();
            for (final VoteOptionRequest opt : optionRequests) {
                if (opt.optionText() == null || opt.optionText().isBlank()) {
                    throw new CustomException(ErrorCode.VOTE_OPTION_TEXT_REQUIRED);
                }
                if (!seenText.add(opt.optionText())) {
                    throw new CustomException(ErrorCode.VOTE_OPTION_TEXT_DUPLICATE);
                }
            }
        }

        final VotePost votePost = VotePost.builder()
                .post(post)
                .endsAt(request.endsAt())
                .build();
        votePostRepository.save(votePost);

        // MBTI 투표인 경우 옵션 수만큼 개별 SELECT 방지 — 한 번에 일괄 조회
        final Map<String, MbtiMetadata> mbtiMap = hasMbti
                ? mbtiMetadataRepository.findAllById(seenMbti).stream()
                        .collect(Collectors.toMap(MbtiMetadata::getId, Function.identity()))
                : Map.of();

        if (hasMbti && mbtiMap.size() != seenMbti.size()) {
            throw new CustomException(ErrorCode.MBTI_NOT_FOUND);
        }

        final List<VotePostOption> options = new ArrayList<>();
        for (int i = 0; i < optionRequests.size(); i++) {
            final VoteOptionRequest optReq = optionRequests.get(i);
            options.add(VotePostOption.builder()
                    .votePost(votePost)
                    .optionText(optReq.optionText())
                    .displayOrder((short) i)
                    .mbtiTarget(hasMbti ? mbtiMap.get(optReq.mbtiTarget()) : null)
                    .build());
        }
        votePostOptionRepository.saveAll(options);

        // votePost가 1차 캐시에 options=[]로 올라가 있어 JOIN FETCH가 캐시를 우선함
        // detach 후 재조회해야 DB에서 options를 올바르게 가져옴
        entityManager.flush();
        entityManager.detach(votePost);
        return votePostRepository.findWithOptionsById(post.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.VOTE_POST_NOT_FOUND));
    }

    /**
     * DebatePost 메타 생성 (트랜잭션 내 호출)
     * 주의: endsAt이 과거 시각이면 즉시 마감 토론이 되므로 차단
     */
    private DebatePost createDebatePostMeta(final Post post, final DebatePostCreateRequest request) {
        if (request.endsAt() != null && request.endsAt().isBefore(Instant.now())) {
            throw new CustomException(ErrorCode.DEBATE_ENDS_AT_INVALID);
        }

        final DebatePost debatePost = DebatePost.builder()
                .post(post)
                .proLabel(request.proLabel() != null && !request.proLabel().isBlank()
                        ? request.proLabel().strip()
                        : "찬성")
                .conLabel(request.conLabel() != null && !request.conLabel().isBlank()
                        ? request.conLabel().strip()
                        : "반대")
                .endsAt(request.endsAt())
                .build();

        return debatePostRepository.save(debatePost);
    }

    @Transactional
    public PostDetailResponse updatePost(final Long id, final PostUpdateRequest request,
            final CustomUserDetails userDetails) {
        final Post post = findPostOrThrow(id);
        verifyOwnership(post, userDetails, request.guestPassword());
        final ProcessedContent pc = processContent(request.content());
        imageCleanupService.markRemovedImages(post.getContent(), pc.normalized());
        post.update(request.title().strip(), pc.normalized(), pc.preview(), pc.imageKeys());

        final UUID currentUserId = userDetails != null ? userDetails.userId() : null;

        if (post.getPostType() == PostType.VOTE) {
            final VotePost votePost = votePostRepository.findWithOptionsById(id)
                    .orElseThrow(() -> new CustomException(ErrorCode.VOTE_POST_NOT_FOUND));
            final VotePostHistory history = (currentUserId != null)
                    ? votePostHistoryRepository.findByVotePost_IdAndUser_Id(id, currentUserId).orElse(null)
                    : null;
            return PostDetailResponse.from(post, currentUserId, votePost, history)
                    .resolveImageUrls(r2PublicUrl);
        }

        if (post.getPostType() == PostType.DEBATE) {
            final DebatePost debatePost = debatePostRepository.findById(id)
                    .orElseThrow(() -> new CustomException(ErrorCode.DEBATE_POST_NOT_FOUND));
            return PostDetailResponse.from(post, currentUserId, debatePost)
                    .resolveImageUrls(r2PublicUrl);
        }

        return PostDetailResponse.from(post, currentUserId).resolveImageUrls(r2PublicUrl);
    }

    @Transactional
    public void deletePost(final Long id, final PostDeleteRequest request,
            final CustomUserDetails userDetails) {
        final Post post = findPostOrThrow(id);
        verifyOwnership(post, userDetails, request.guestPassword());
        imageCleanupService.markRemovedImages(post.getContent(), "");
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

    private record ProcessedContent(String normalized, String preview, String[] imageKeys) {}

    private ProcessedContent processContent(final String rawContent) {
        final String normalized = TiptapContentParser.normalizeForStorage(rawContent, r2PublicUrl);
        if (TiptapContentParser.countImages(normalized) > PostConstants.MAX_IMAGE_COUNT) {
            throw new CustomException(ErrorCode.IMAGE_COUNT_EXCEEDED);
        }
        return new ProcessedContent(
                normalized,
                TiptapContentParser.extractTextPreview(normalized, 300),
                TiptapContentParser.extractImageKeys(normalized, PostConstants.MAX_IMAGE_COUNT)
        );
    }

    private Post findPostOrThrow(final Long id) {
        return postRepository.findWithDetailsById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
    }

    private User findUserOrThrow(final UUID userId) {
        return userRepository.findWithMbtiById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
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
