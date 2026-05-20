package com.board.kimicetea.domain.post.service;

import com.board.kimicetea.domain.mbti.entity.MbtiMetadata;
import com.board.kimicetea.domain.mbti.repository.MbtiMetadataRepository;
import com.board.kimicetea.domain.post.dto.PostCreateRequest;
import com.board.kimicetea.domain.post.dto.PostDeleteRequest;
import com.board.kimicetea.domain.post.dto.PostDetailResponse;
import com.board.kimicetea.domain.post.dto.PostSummaryResponse;
import com.board.kimicetea.domain.post.dto.PostUpdateRequest;
import com.board.kimicetea.domain.post.entity.Post;
import com.board.kimicetea.domain.post.entity.PostType;
import com.board.kimicetea.domain.post.repository.PostRepository;
import com.board.kimicetea.domain.post.repository.PostTypeRepository;
import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.domain.user.security.CustomUserDetails;
import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 게시글 도메인 비즈니스 로직 (목록·단건 조회, 작성·수정·삭제)
 * 주의: 회원/비회원 분기는 userDetails null 여부로 판단
 */
@Service
@RequiredArgsConstructor
public class PostService {

    private static final String NONE_MBTI_ID = "NONE";

    private final PostRepository postRepository;
    private final PostTypeRepository postTypeRepository;
    private final MbtiMetadataRepository mbtiMetadataRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> getPosts(final String type, final String sort,
                                              final String authorMbti, final Pageable pageable) {
        return postRepository.findPosts(type, sort, authorMbti, pageable)
                .map(PostSummaryResponse::from);
    }

    /**
     * 단건 조회 + viewCount 원자적 증가
     * 주의: @Modifying 쿼리가 포함되므로 readOnly = false
     */
    @Transactional
    public PostDetailResponse getPost(final Long id, final UUID currentUserId) {
        final Post post = findPostOrThrow(id);
        postRepository.increaseViewCount(id);
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
            if (request.guestPassword() == null || request.guestPassword().isBlank()) {
                throw new CustomException(ErrorCode.GUEST_PASSWORD_REQUIRED);
            }
            user = null;
            authorMbti = findNoneMbtiOrThrow();
        }

        final Post post = Post.builder()
                .title(request.title())
                .content(request.content())
                .guestPassword(user == null ? passwordEncoder.encode(request.guestPassword()) : null)
                .user(user)
                .authorMbti(authorMbti)
                .postType(postType)
                .build();

        final Post saved = postRepository.save(post);
        return PostDetailResponse.from(saved, userDetails != null ? userDetails.userId() : null);
    }

    @Transactional
    public PostDetailResponse updatePost(final Long id, final PostUpdateRequest request,
                                          final CustomUserDetails userDetails) {
        final Post post = findPostOrThrow(id);
        verifyOwnership(post, userDetails, request.guestPassword());
        post.update(request.title(), request.content());
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
        final boolean isGuestPost = post.getGuestPassword() != null;

        if (isGuestPost) {
            if (rawGuestPassword == null || rawGuestPassword.isBlank()) {
                throw new CustomException(ErrorCode.GUEST_PASSWORD_REQUIRED);
            }
            if (!passwordEncoder.matches(rawGuestPassword, post.getGuestPassword())) {
                throw new CustomException(ErrorCode.GUEST_PASSWORD_MISMATCH);
            }
            return;
        }

        // 회원 작성 글
        if (post.getUser() == null || userDetails == null
                || !post.getUser().getId().equals(userDetails.userId())) {
            throw new CustomException(ErrorCode.POST_FORBIDDEN);
        }
    }

    private Post findPostOrThrow(final Long id) {
        return postRepository.findWithDetailsById(id)
                .filter(Post::getIsVisible)
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
        return mbtiMetadataRepository.findById(NONE_MBTI_ID)
                .orElseThrow(() -> new CustomException(ErrorCode.MBTI_NOT_FOUND));
    }
}
