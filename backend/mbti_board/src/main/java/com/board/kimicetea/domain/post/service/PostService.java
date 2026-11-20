package com.board.kimicetea.domain.post.service;

import com.board.kimicetea.domain.mbti.MbtiMetadataSupport;
import com.board.kimicetea.domain.mbti.entity.MbtiMetadata;
import com.board.kimicetea.domain.mbti.repository.MbtiMetadataRepository;
import com.board.kimicetea.domain.post.PostConstants;
import com.board.kimicetea.global.common.util.GuestPasswordHelper;
import com.board.kimicetea.global.common.util.RequestUtils;
import com.board.kimicetea.global.common.util.TiptapContentParser;
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
import com.board.kimicetea.domain.post.entity.PostImage;
import com.board.kimicetea.domain.post.entity.PostType;
import com.board.kimicetea.domain.post.enums.PostImageStatus;
import com.board.kimicetea.domain.post.entity.VotePost;

import com.board.kimicetea.domain.post.entity.VotePostHistory;
import com.board.kimicetea.domain.post.entity.VotePostOption;
import com.board.kimicetea.domain.post.repository.DebatePostRepository;
import com.board.kimicetea.domain.post.repository.DebateStanceHistoryRepository;
import com.board.kimicetea.domain.post.repository.PostImageRepository;
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
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
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
    private final PostImageRepository postImageRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${image-server.public-url}")
    private String r2PublicUrl;

    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> getPosts(final String type, final PostSortType sort,
            final String authorMbti, final String targetMbti,
            final Pageable pageable) {
        final PostType postType = PostType.fromNullable(type).orElse(null);
        return postRepository.findPosts(postType, sort, authorMbti, targetMbti, pageable)
                .map(post -> PostSummaryResponse.from(post, r2PublicUrl));
    }

    /** 내 게시글 목록 — typeCode null 이면 전체 유형 반환 */
    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> getMyPosts(final UUID userId, final String typeCode,
            final PostSortType sort, final Pageable pageable) {
        final PostType postType = PostType.fromNullable(typeCode).orElse(null);
        return postRepository.findMyPosts(userId, postType, sort, pageable)
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

        // 투표글: 선택지와 현재 사용자의 투표 이력을 함께 조회해 응답에 포함
        if (post.getPostType() == PostType.VOTE) {
            // 선택지는 fetch join으로 한 번에 로딩 (옵션별 추가 쿼리 방지)
            final VotePost votePost = votePostRepository.findWithOptionsById(id)
                    .orElseThrow(() -> new CustomException(ErrorCode.VOTE_POST_NOT_FOUND));
            // 비회원이면 이력 조회 없이 null — 응답에서 "투표 안 함"으로 처리됨
            final VotePostHistory history = (currentUserId != null)
                    ? votePostHistoryRepository.findByVotePost_IdAndUser_Id(id, currentUserId).orElse(null)
                    : null;
            // 본문의 이미지 키를 공개 URL로 변환해서 응답 (아래 분기도 동일)
            return PostDetailResponse.from(post, currentUserId, votePost, history)
                    .resolveImageUrls(r2PublicUrl);
        }

        // 토론글: 토론 정보와 현재 사용자가 선택한 진영(1/-1)을 함께 조회
        if (post.getPostType() == PostType.DEBATE) {
            final DebatePost debatePost = debatePostRepository.findById(id)
                    .orElseThrow(() -> new CustomException(ErrorCode.DEBATE_POST_NOT_FOUND));
            // 비회원이거나 아직 진영을 고르지 않았으면 null
            final Short myStance = (currentUserId != null)
                    ? debateStanceHistoryRepository.findStanceByUserIdAndPostId(currentUserId, id).orElse(null)
                    : null;
            return PostDetailResponse.from(post, currentUserId, debatePost, myStance)
                    .resolveImageUrls(r2PublicUrl);
        }

        // 자유글: 투표·토론 부가 정보 없이 기본 응답만 생성
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
        final Set<String> imageIds = TiptapContentParser.extractImageIds(request.content(), r2PublicUrl);
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
        attachImages(saved.getId(), imageIds);
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
        final Set<String> imageIds = TiptapContentParser.extractImageIds(request.content(), r2PublicUrl);
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
        final DebatePost debatePost = createDebatePostMeta(saved, request);
        attachImages(saved.getId(), imageIds);
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
        final Set<String> imageIds = TiptapContentParser.extractImageIds(request.content(), r2PublicUrl);
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
        final VotePost votePost = createVotePostMeta(saved, request);
        attachImages(saved.getId(), imageIds);
        return PostDetailResponse.from(saved, userDetails.userId(), votePost, null)
                .resolveImageUrls(r2PublicUrl);
    }

    /**
     * VotePost + VotePostOption 생성 (트랜잭션 내 호출)
     * 주의:
     * - voteOptions 개수는 요청 DTO(@NotNull/@Size)에서 검증 — 컨트롤러 외 경로로 호출하면 직접 검증 필요
     * - MBTI 투표 시 같은 mbtiTarget 중복 불가 (DB 인덱스 + 서비스 사전 검증)
     */
    private VotePost createVotePostMeta(final Post post, final VotePostCreateRequest request) {
        // 선택지 개수(2~VOTE_OPTION_MAX)는 VotePostCreateRequest의 Bean Validation에서 이미 검증됨
        final List<VoteOptionRequest> optionRequests = request.voteOptions();

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

        // 투표 유형별 선택지 검증 — Set.add()가 false면 이미 본 값이라 중복
        final Set<String> seenMbti = new HashSet<>();
        if (hasMbti) {
            // MBTI 투표: 같은 MBTI를 두 번 고를 수 없음 (mbtiTarget이 곧 선택지의 정체성)
            for (final VoteOptionRequest opt : optionRequests) {
                if (!seenMbti.add(opt.mbtiTarget())) {
                    throw new CustomException(ErrorCode.VOTE_MBTI_DUPLICATE);
                }
            }
        } else {
            // 자유 투표: 모든 선택지에 텍스트가 있어야 하고, 같은 텍스트는 중복 불가
            final Set<String> seenText = new HashSet<>();
            for (final VoteOptionRequest opt : optionRequests) {
                if (!StringUtils.hasText(opt.optionText())) {
                    throw new CustomException(ErrorCode.VOTE_OPTION_TEXT_REQUIRED);
                }
                if (!seenText.add(opt.optionText())) {
                    throw new CustomException(ErrorCode.VOTE_OPTION_TEXT_DUPLICATE);
                }
            }
        }

        // 위 검증을 모두 통과한 뒤에야 저장 — VotePost는 posts.id를 PK로 공유(@MapsId)하는 1:1 메타
        final VotePost votePost = VotePost.builder()
                .post(post)
                .endsAt(request.endsAt())
                .build();
        votePostRepository.save(votePost);

        // MBTI 투표인 경우 옵션 수만큼 개별 SELECT 방지 — 한 번에 일괄 조회
        // (id → 엔티티 Map으로 만들어 아래 옵션 생성 때 O(1)로 찾음)
        final Map<String, MbtiMetadata> mbtiMap = hasMbti
                ? mbtiMetadataRepository.findAllById(seenMbti).stream()
                        .collect(Collectors.toMap(MbtiMetadata::getId, Function.identity()))
                : Map.of();

        // 요청한 MBTI 중 DB에 없는 값이 있으면 조회된 개수가 요청 개수보다 적음
        if (hasMbti && mbtiMap.size() != seenMbti.size()) {
            throw new CustomException(ErrorCode.MBTI_NOT_FOUND);
        }

        // 요청 순서대로 displayOrder(0부터)를 부여 — 조회 시 @OrderBy("displayOrder ASC")로 이 순서가 복원됨
        // 자유 투표는 mbtiTarget이 null (MBTI 투표만 위에서 조회한 엔티티를 연결)
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
        // PK가 IDENTITY라 saveAll 시점에 INSERT가 즉시 나가고, options의 id가 채워짐
        votePostOptionRepository.saveAll(options);

        // votePost는 위에서 options=[]인 채로 1차 캐시에 올라가 있음 — 방금 저장한 선택지를 직접 채워서 반환
        // (flush·detach 후 JOIN FETCH로 다시 읽는 방식 대신 SELECT 1회 절약)
        // 주의: 선택지의 vote_count는 DB 트리거 관리 컬럼이지만 신규 생성 시 항상 0이고 엔티티 기본값(0)과 같아서
        //       다시 읽지 않아도 일치함. 선택지에 DB가 채우는 컬럼이 추가되면 이 방식은 그 값을 못 봄
        votePost.getOptions().addAll(options);
        return votePost;
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
                .proLabel(StringUtils.hasText(request.proLabel())
                        ? request.proLabel().strip()
                        : "찬성")
                .conLabel(StringUtils.hasText(request.conLabel())
                        ? request.conLabel().strip()
                        : "반대")
                .endsAt(request.endsAt())
                .build();

        return debatePostRepository.save(debatePost);
    }

    /**
     * PATCH /api/posts/{id} — 게시글 수정 (회원·비회원 모두 가능)
     * [역할] 제목·본문을 바꾸고, 본문에서 이미지가 추가·삭제된 만큼 post_images 소유 상태를 맞춘 뒤 수정 결과를 반환
     * 주의:
     * - 수정되는 건 제목·본문(과 거기서 뽑은 요약·이미지 key)뿐 — 유형, 대상 MBTI, 투표 선택지, 토론 설정은 바뀌지 않음
     * - 이미지는 post_images 상태만 바꾸고 Image Server confirm/release는 커밋 뒤 워커가 함 (트랜잭션 안에서 외부 호출 없음)
     * - post.update는 save 없이 변경 감지로 커밋 시 UPDATE됨 — 이 트랜잭션이 롤백되면 이미지 상태 변경도 함께 취소
     */
    @Transactional
    public PostDetailResponse updatePost(final Long id, final PostUpdateRequest request,
            final CustomUserDetails userDetails) {
        final Post post = findPostOrThrow(id);
        // 회원은 작성자 일치, 비회원은 글 비밀번호 일치여야 함 (탈퇴 회원의 글은 수정 불가)
        verifyOwnership(post, userDetails, request.guestPassword());
        // 새 본문을 저장 형태로 가공 — 이미지 개수를 넘으면 여기서 예외, 이후 모든 비교는 가공된 본문 기준
        final ProcessedContent pc = processContent(request.content());
        // 본문이 아니라 소유 기록 기준으로 비교 — 본문에 남의 이미지가 들어 있어도 그건 정리 대상이 아니다.
        // 여기서는 post_images 상태만 바꾸고, Image Server confirm/release는 커밋 뒤 워커가 한다.
        final Set<String> newIds = TiptapContentParser.extractImageIds(pc.normalized(), r2PublicUrl);
        final Map<String, PostImageStatus> owned = findOwnedImages(id);
        // 1) 새 본문에 처음 나온 이미지(아직 이 글의 소유 기록이 없음) → 소유 등록
        attachImages(id, difference(newIds, owned.keySet()));
        // 2) 지웠다가(정리 대기 중) 다시 넣은 이미지 → 정리 대상에서 빼고 확정 대상으로 되돌림
        reactivateImages(id, newIds.stream()
                .filter(imageId -> owned.get(imageId) == PostImageStatus.PENDING_RELEASE)
                .collect(Collectors.toSet()));
        // 3) 이 글이 소유 중이던 이미지 중 새 본문에서 빠진 것 → 정리 대상으로 표시
        detachImages(id, owned.entrySet().stream()
                .filter(entry -> entry.getValue().isAttached() && !newIds.contains(entry.getKey()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet()));
        post.update(request.title().strip(), pc.normalized(), pc.preview(), pc.imageKeys());

        // 비회원이면 null — 응답의 isOwner 등 "내 것" 판단과 투표 이력·내 진영 조회에 쓰임
        final UUID currentUserId = userDetails != null ? userDetails.userId() : null;

        // 상세 조회와 같은 변환을 재사용 — 유형별 부가 정보(투표 선택지·내 투표 이력, 토론 정보·내 진영)까지 동일하게 반환
        return buildDetailResponse(post, currentUserId);
    }

    @Transactional
    public void deletePost(final Long id, final PostDeleteRequest request,
            final CustomUserDetails userDetails) {
        final Post post = findPostOrThrow(id);
        verifyOwnership(post, userDetails, request.guestPassword());
        releaseImages(post);
        post.hide();
    }

    /**
     * 게시글이 소유한 이미지를 정리 대상(PENDING_RELEASE)으로 표시 — 실제 삭제는 커밋 뒤 워커가 한다.
     * AdminPostService의 영구 삭제에서도 재사용(표시 후 글을 지우면 FK가 post_id를 NULL로 두고 행은 남는다).
     * 주의: 본문에 있는 이미지가 아니라 소유 기록(post_images) 기준 — 남의 글 이미지 URL을 본문에 넣은 글을 지워도
     * 원래 글의 이미지는 지워지지 않는다
     */
    void releaseImages(final Post post) {
        detachImages(post.getId(), findOwnedImages(post.getId()).keySet());
    }

    /**
     * 본문 이미지 중 아직 어느 글도 소유하지 않은 것만 이 글 소유로 등록한다(PENDING_CONFIRM).
     * 이미 다른 글이 소유한 이미지는 무시 — 보여주기만 하고 확정·정리 대상이 아니다. 확정은 커밋 뒤 워커가 한다.
     */
    private void attachImages(final Long postId, final Set<String> imageIds) {
        for (final String imageId : imageIds) {
            postImageRepository.claim(UUID.fromString(imageId), postId);
        }
    }

    /** 뺐다가 다시 넣은 이미지(아직 정리 전)를 확정 대상으로 되돌린다 */
    private void reactivateImages(final Long postId, final Set<String> imageIds) {
        if (!imageIds.isEmpty()) {
            postImageRepository.reactivate(postId, toUuids(imageIds));
        }
    }

    /** 이 글이 소유한 이미지 중 imageIds를 정리 대상으로 표시한다 */
    private void detachImages(final Long postId, final Set<String> imageIds) {
        if (!imageIds.isEmpty()) {
            postImageRepository.markPendingRelease(postId, toUuids(imageIds));
        }
    }

    /** 이 글이 소유한 이미지 id → 상태 */
    private Map<String, PostImageStatus> findOwnedImages(final Long postId) {
        return postImageRepository.findByPostId(postId).stream()
                .collect(Collectors.toMap(image -> image.getImageId().toString(), PostImage::getStatus));
    }

    private static List<UUID> toUuids(final Set<String> imageIds) {
        return imageIds.stream().map(UUID::fromString).toList();
    }

    private static Set<String> difference(final Collection<String> source, final Collection<String> exclude) {
        final Set<String> result = new HashSet<>(source);
        result.removeAll(exclude);
        return result;
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

    /**
     * 본문 가공 결과 — posts 테이블에 함께 저장할 값 묶음
     * normalized → content, preview → content_preview, imageKeys → image_urls
     * 주의: imageKeys는 본문에 이미지가 없으면 null
     */
    private record ProcessedContent(String normalized, String preview, String[] imageKeys) {}

    /**
     * 요청 본문(Tiptap JSON)을 저장 가능한 형태로 가공 — 작성·수정 시 공통으로 호출
     * [역할]
     * - 본문 안의 실제 R2 이미지 URL을 {public-url}/ 플레이스홀더로 치환 (저장소를 옮겨도 설정값 하나로 대응)
     * - 목록·SEO 메타용으로 본문 요약(preview)과 이미지 key를 본문과 별도 컬럼에 미리 뽑아 둠
     *   (조회 때마다 큰 본문 JSON을 파싱하지 않도록)
     * 주의:
     * - 이미지 개수 검증은 치환된 본문 기준이며, 초과하면 저장 전에 IMAGE_COUNT_EXCEEDED로 막음
     * - preview 길이(300자)와 imageKeys 최대 개수(MAX_IMAGE_COUNT)는 여기서만 정함
     * - 이미지 소유 등록(claim)은 하지 않음 — 글 저장 후 attachImages가 따로 처리
     */
    private ProcessedContent processContent(final String rawContent) {
        // 저장용 본문: 이후 모든 추출은 이 치환된 본문을 기준으로 함
        final String normalized = TiptapContentParser.normalizeForStorage(rawContent, r2PublicUrl);

        // DB에 쓰기 전에 개수부터 검증 — 초과한 글이 저장되지 않게 함
        if (TiptapContentParser.countImages(normalized) > PostConstants.MAX_IMAGE_COUNT) {
            throw new CustomException(ErrorCode.IMAGE_COUNT_EXCEEDED);
        }
        return new ProcessedContent(
                normalized,
                // 본문의 text 노드만 모아 300자까지 — 목록 카드·OG 설명에 사용
                TiptapContentParser.extractTextPreview(normalized, 300),
                // 문서 순서대로 최대 MAX_IMAGE_COUNT개 — 대표 이미지(OG 등) 용도
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
        if (!StringUtils.hasText(targetMbtiId) || PostConstants.TARGET_ALL.equals(targetMbtiId)) {
            return null;
        }
        return mbtiMetadataRepository.findById(targetMbtiId)
                .orElseThrow(() -> new CustomException(ErrorCode.MBTI_NOT_FOUND));
    }
}
