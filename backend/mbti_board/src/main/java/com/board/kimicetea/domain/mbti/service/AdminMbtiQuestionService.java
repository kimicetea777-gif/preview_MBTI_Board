package com.board.kimicetea.domain.mbti.service;

import com.board.kimicetea.domain.mbti.dto.AdminMbtiQuestionCreateRequest;
import com.board.kimicetea.domain.mbti.dto.AdminMbtiQuestionResponse;
import com.board.kimicetea.domain.mbti.dto.AdminMbtiQuestionUpdateRequest;
import com.board.kimicetea.domain.mbti.dto.AdminMbtiQuestionVisibilityRequest;
import com.board.kimicetea.domain.mbti.entity.MbtiQuestion;
import com.board.kimicetea.domain.mbti.repository.MbtiQuestionRepository;
import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 어드민 MBTI 문항 관리 서비스 — 전체 조회·생성·수정·노출 변경 담당
 * 주의: testVersion·id는 변경 불가, title·category·isReverse만 수정 허용
 */
@Service
@RequiredArgsConstructor
public class AdminMbtiQuestionService {

    private static final String TEST_VERSION = "v1.0";

    private final MbtiQuestionRepository mbtiQuestionRepository;

    /** 전체 문항 조회 — is_visible 무관, 카테고리·ID 오름차순 */
    @Transactional(readOnly = true)
    public List<AdminMbtiQuestionResponse> getAllQuestions() {
        return mbtiQuestionRepository.findAllByTestVersionOrderByCategoryAscIdAsc(TEST_VERSION)
                .stream()
                .map(AdminMbtiQuestionResponse::from)
                .toList();
    }

    /**
     * MBTI 문항 생성 — testVersion은 항상 "v1.0" 고정, isVisible 기본 true
     * 주의: 카테고리별 채점 균형이 검사 로직(MbtiService)의 전제라, category·isReverse를 신중히 입력해야 함
     */
    @Transactional
    public AdminMbtiQuestionResponse createQuestion(final AdminMbtiQuestionCreateRequest request) {
        final MbtiQuestion question = MbtiQuestion.builder()
                .title(request.title())
                .category(request.category())
                .isReverse(request.isReverse())
                .testVersion(TEST_VERSION)
                .build();
        return AdminMbtiQuestionResponse.from(mbtiQuestionRepository.save(question));
    }

    /** MBTI 문항 수정 — 존재하지 않는 ID면 QUESTION_NOT_FOUND 예외 */
    @Transactional
    public void updateQuestion(final Long id, final AdminMbtiQuestionUpdateRequest request) {
        final MbtiQuestion question = mbtiQuestionRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.QUESTION_NOT_FOUND));
        question.update(request.title(), request.category(), request.isReverse());
    }

    /** MBTI 문항 노출 여부 변경 */
    @Transactional
    public void changeVisibility(final Long id, final AdminMbtiQuestionVisibilityRequest request) {
        final MbtiQuestion question = mbtiQuestionRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.QUESTION_NOT_FOUND));
        question.changeVisibility(request.visible());
    }
}
