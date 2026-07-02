package com.board.kimicetea.domain.post.service;

import com.board.kimicetea.domain.post.dto.VoteRequest;
import com.board.kimicetea.domain.post.dto.VoteResultResponse;
import com.board.kimicetea.domain.post.entity.VotePost;
import com.board.kimicetea.domain.post.entity.VotePostHistory;
import com.board.kimicetea.domain.post.entity.VotePostOption;
import com.board.kimicetea.domain.post.repository.PostRepository;
import com.board.kimicetea.domain.post.repository.VotePostHistoryRepository;
import com.board.kimicetea.domain.post.repository.VotePostOptionRepository;
import com.board.kimicetea.domain.post.repository.VotePostRepository;
import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 투표 참여 및 결과 조회 비즈니스 로직
 * 주의:
 * - castVote는 회원 전용 — SecurityConfig에서 authenticated()로 보호되지만 서비스에서도 null 검증
 * - vote_count는 DB 트리거가 갱신하므로 flush()+clear() 후 재조회로 최신값 반환
 * - 선택지가 해당 게시글 소속인지 반드시 검증 (VOTE_OPTION_NOT_BELONGS)
 */
@Service
@RequiredArgsConstructor
public class VoteService {

    private final VotePostRepository votePostRepository;
    private final VotePostOptionRepository votePostOptionRepository;
    private final VotePostHistoryRepository votePostHistoryRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 투표 참여 — 기존 투표가 있으면 선택지 변경, 없으면 신규 등록
     * 주의: 마감된 투표·숨겨진 게시글은 400/404, 다른 게시글 선택지 ID 사용 시 400
     */
    @Transactional
    public VoteResultResponse castVote(final Long postId, final VoteRequest request,
            final UUID userId) {

        // 숨겨진(is_visible=false) 게시글에 투표 방지
        if (!postRepository.existsByIdAndIsVisibleTrue(postId)) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }

        final VotePost votePost = findVotePostWithOptionsOrThrow(postId);
        validateNotEnded(votePost);

        final VotePostOption option = findOptionOrThrow(request.optionId());
        validateOptionBelongsToPost(option, postId);

        final User user = findUserOrThrow(userId);

        final var existing = votePostHistoryRepository.findByVotePost_IdAndUser_Id(postId, user.getId());
        if (existing.isPresent()) {
            existing.get().changeOption(option);
        } else {
            final VotePostHistory newHistory = VotePostHistory.builder()
                    .votePost(votePost)
                    .votePostOption(option)
                    .user(user)
                    .build();
            votePostHistoryRepository.save(Objects.requireNonNull(newHistory));
        }

        // flush로 DB 트리거(sync_vote_count) 실행, clear로 1차 캐시 무효화 후 최신 상태 재조회
        entityManager.flush();
        entityManager.clear();

        final VotePost updated = findVotePostWithOptionsOrThrow(postId);
        final VotePostHistory history = votePostHistoryRepository
                .findByVotePost_IdAndUser_Id(postId, userId).orElseThrow();
        return VoteResultResponse.from(updated, history);
    }

    /**
     * 투표 결과 조회 — 로그인 유저는 본인 선택 옵션 포함
     */
    @Transactional(readOnly = true)
    public VoteResultResponse getVoteResult(final Long postId, final UUID currentUserId) {
        if (!postRepository.existsByIdAndIsVisibleTrue(postId)) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }

        final VotePost votePost = findVotePostWithOptionsOrThrow(postId);
        final VotePostHistory history = (currentUserId != null)
                ? votePostHistoryRepository.findByVotePost_IdAndUser_Id(postId, currentUserId).orElse(null)
                : null;

        return VoteResultResponse.from(votePost, history);
    }

    private void validateNotEnded(final VotePost votePost) {
        if (votePost.getEndsAt() != null && Instant.now().isAfter(votePost.getEndsAt())) {
            throw new CustomException(ErrorCode.VOTE_ENDED);
        }
    }

    /** 선택지가 요청된 postId의 게시글 소속인지 검증 — 다른 게시글 선택지 ID 주입 방지 */
    private void validateOptionBelongsToPost(final VotePostOption option, final Long postId) {
        if (!option.getVotePost().getId().equals(postId)) {
            throw new CustomException(ErrorCode.VOTE_OPTION_NOT_BELONGS);
        }
    }

    private VotePost findVotePostWithOptionsOrThrow(final Long postId) {
        return votePostRepository.findWithOptionsById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.VOTE_POST_NOT_FOUND));
    }

    private VotePostOption findOptionOrThrow(final Long optionId) {
        return votePostOptionRepository.findById(Objects.requireNonNull(optionId))
                .orElseThrow(() -> new CustomException(ErrorCode.VOTE_OPTION_NOT_FOUND));
    }

    private User findUserOrThrow(final UUID userId) {
        return userRepository.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
