package com.board.kimicetea.domain.post.service;

import com.board.kimicetea.domain.mbti.MbtiMetadataSupport;
import com.board.kimicetea.domain.mbti.entity.MbtiMetadata;
import com.board.kimicetea.domain.post.dto.PostCreateRequest;
import com.board.kimicetea.domain.post.dto.PostDeleteRequest;
import com.board.kimicetea.domain.post.dto.PostUpdateRequest;
import com.board.kimicetea.domain.post.entity.Post;
import com.board.kimicetea.domain.post.entity.PostImage;
import com.board.kimicetea.domain.post.entity.PostType;
import com.board.kimicetea.domain.post.enums.PostImageStatus;
import com.board.kimicetea.domain.post.repository.PostImageRepository;
import com.board.kimicetea.domain.post.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 게시글 저장·수정·삭제가 post_images 상태만 바꾸는지(Image Server 호출은 워커 몫) 확인한다.
 * PostService는 ImageServerClient에 의존하지 않으므로, 트랜잭션 안 외부 호출이 없다는 것은 컴파일 단계에서 보장된다.
 * 이미지 id는 공개 URL에 들어 있어 남의 이미지를 본문에 넣을 수 있으므로, 정리 대상은 소유한 이미지뿐이어야 한다.
 */
@ExtendWith(MockitoExtension.class)
class PostServiceImageTest {

    private static final String PUBLIC_URL = "https://images.test";
    private static final long POST_ID = 1L;
    private static final String GUEST_PW = "pw1234";

    private static final String A = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String B = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    private static final String C = "cccccccc-cccc-cccc-cccc-cccccccccccc";
    private static final String D = "dddddddd-dddd-dddd-dddd-dddddddddddd";
    private static final String F = "ffffffff-ffff-ffff-ffff-ffffffffffff";
    private static final String OTHERS = "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee";

    @Mock private PostRepository postRepository;
    @Mock private PostImageRepository postImageRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private MbtiMetadataSupport mbtiMetadataSupport;

    private PostService postService;

    @BeforeEach
    void setUp() {
        postService = new PostService(postRepository, null, mbtiMetadataSupport, null, passwordEncoder,
                null, null, null, null, null, postImageRepository);
        ReflectionTestUtils.setField(postService, "r2PublicUrl", PUBLIC_URL);
    }

    @Test
    @DisplayName("작성: 본문 이미지를 소유 등록(claim)만 한다 — 확정은 커밋 뒤 워커가 한다")
    void createOnlyClaims() {
        final MbtiMetadata none = mock(MbtiMetadata.class);
        lenient().when(none.getId()).thenReturn("NONE");
        when(mbtiMetadataSupport.getNoneMbti()).thenReturn(none);
        final Post saved = mock(Post.class);
        when(saved.getId()).thenReturn(POST_ID);
        when(saved.getPostType()).thenReturn(PostType.FREE);
        when(saved.getAuthorMbti()).thenReturn(none);
        when(postRepository.save(any(Post.class))).thenReturn(saved);

        postService.createPost(
                new PostCreateRequest("제목", contentWith(A, OTHERS), "FREE", GUEST_PW, null, true, true, true),
                null, new MockHttpServletRequest());

        verify(postImageRepository).claim(UUID.fromString(A), POST_ID);
        verify(postImageRepository).claim(UUID.fromString(OTHERS), POST_ID);
        verify(postImageRepository, never()).markPendingRelease(any(), anyCollection());
    }

    @Test
    @DisplayName("수정: 새 이미지는 claim, 뺐다 다시 넣은 이미지는 되돌림, 빠진 소유 이미지는 정리 대상으로 표시")
    void updateTransitionsByOwnership() {
        final Post post = guestPost();
        owns(Map.of(
                A, PostImageStatus.CONFIRMED,         // 빠짐 → 정리 대상
                B, PostImageStatus.CONFIRMED,         // 유지
                D, PostImageStatus.PENDING_RELEASE,   // 다시 넣음 → 되돌림
                F, PostImageStatus.CONFIRM_FAILED));  // 빠짐 → 정리 대상

        postService.updatePost(POST_ID, new PostUpdateRequest("제목", contentWith(B, C, D), GUEST_PW), null);

        verify(postImageRepository).claim(UUID.fromString(C), POST_ID);
        verify(postImageRepository, never()).claim(eq(UUID.fromString(B)), any());
        verify(postImageRepository, never()).claim(eq(UUID.fromString(D)), any());
        assertThat(captureIds("reactivate")).containsExactlyInAnyOrder(uuid(D));
        assertThat(captureIds("markPendingRelease")).containsExactlyInAnyOrder(uuid(A), uuid(F));
        verify(post).update(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("수정: 이미 다른 글이 소유한 이미지는 본문에 넣었다 빼도 이 글의 정리 대상이 아니다")
    void updateIgnoresImagesOwnedByOtherPosts() {
        guestPost();
        owns(Map.of());

        postService.updatePost(POST_ID, new PostUpdateRequest("제목", contentWith(OTHERS), GUEST_PW), null);

        verify(postImageRepository).claim(UUID.fromString(OTHERS), POST_ID);
        verify(postImageRepository, never()).markPendingRelease(any(), anyCollection());
        verify(postImageRepository, never()).reactivate(any(), anyCollection());
    }

    @Test
    @DisplayName("삭제: 본문에 남의 이미지가 있어도 이 글이 소유한 이미지만 정리 대상으로 표시한다")
    void deleteMarksOnlyOwnedImages() {
        final Post post = guestPost();
        lenient().when(post.getContent()).thenReturn(contentWith(A, OTHERS));
        owns(Map.of(A, PostImageStatus.CONFIRMED));

        postService.deletePost(POST_ID, new PostDeleteRequest(GUEST_PW), null);

        assertThat(captureIds("markPendingRelease")).containsExactly(uuid(A));
        verify(post).hide();
    }

    @Test
    @DisplayName("관리자 영구 삭제 경로(releaseImages)도 소유 이미지를 정리 대상으로 표시한다")
    void releaseImagesMarksOwnedImages() {
        final Post post = mock(Post.class);
        when(post.getId()).thenReturn(POST_ID);
        owns(Map.of(A, PostImageStatus.PENDING_CONFIRM, B, PostImageStatus.CONFIRMED));

        postService.releaseImages(post);

        assertThat(captureIds("markPendingRelease")).containsExactlyInAnyOrder(uuid(A), uuid(B));
    }

    // ------------------------------------------------------------------------------------------

    /** 비회원 글(비밀번호로 소유 확인) — 회원 조회 없이 수정·삭제 경로를 탈 수 있다 */
    private Post guestPost() {
        final Post post = mock(Post.class);
        final MbtiMetadata mbti = mock(MbtiMetadata.class);
        lenient().when(post.getId()).thenReturn(POST_ID);
        lenient().when(post.getGuestPassword()).thenReturn("hashed");
        lenient().when(post.getPostType()).thenReturn(PostType.FREE);
        lenient().when(post.getAuthorMbti()).thenReturn(mbti);
        lenient().when(mbti.getId()).thenReturn("NONE");
        when(postRepository.findWithDetailsById(POST_ID)).thenReturn(Optional.of(post));
        when(passwordEncoder.matches(GUEST_PW, "hashed")).thenReturn(true);
        return post;
    }

    private void owns(final Map<String, PostImageStatus> images) {
        final List<PostImage> rows = images.entrySet().stream()
                .map(entry -> {
                    final PostImage image = mock(PostImage.class);
                    lenient().when(image.getImageId()).thenReturn(UUID.fromString(entry.getKey()));
                    lenient().when(image.getStatus()).thenReturn(entry.getValue());
                    return image;
                })
                .toList();
        when(postImageRepository.findByPostId(POST_ID)).thenReturn(rows);
    }

    @SuppressWarnings("unchecked")
    private Collection<UUID> captureIds(final String method) {
        final ArgumentCaptor<Collection<UUID>> captor = ArgumentCaptor.forClass(Collection.class);
        if ("reactivate".equals(method)) {
            verify(postImageRepository).reactivate(eq(POST_ID), captor.capture());
        } else {
            verify(postImageRepository).markPendingRelease(eq(POST_ID), captor.capture());
        }
        return captor.getValue();
    }

    private static UUID uuid(final String id) {
        return UUID.fromString(id);
    }

    /** Tiptap 본문 — 이미지 src는 에디터가 넣는 원본 공개 URL 형식 */
    private static String contentWith(final String... imageIds) {
        final String images = Arrays.stream(imageIds)
                .map(id -> "{\"type\":\"image\",\"attrs\":{\"src\":\"" + PUBLIC_URL + "/mbti_board/" + id + ".webp\"}}")
                .collect(Collectors.joining(","));
        return "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"본문\"}]},"
                + images + "]}";
    }
}
