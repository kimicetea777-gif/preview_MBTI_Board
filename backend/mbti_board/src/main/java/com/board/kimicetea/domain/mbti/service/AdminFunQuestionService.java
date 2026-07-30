package com.board.kimicetea.domain.mbti.service;

import com.board.kimicetea.domain.mbti.dto.AdminFunOptionCreateRequest;
import com.board.kimicetea.domain.mbti.dto.AdminFunOptionUpdateRequest;
import com.board.kimicetea.domain.mbti.dto.AdminFunQuestionCreateRequest;
import com.board.kimicetea.domain.mbti.dto.AdminFunQuestionResponse;
import com.board.kimicetea.domain.mbti.dto.AdminFunQuestionUpdateRequest;
import com.board.kimicetea.domain.mbti.dto.AdminFunQuestionVisibilityRequest;
import com.board.kimicetea.domain.mbti.entity.FunQuestion;
import com.board.kimicetea.domain.mbti.entity.FunQuestionOption;
import com.board.kimicetea.domain.mbti.repository.FunQuestionOptionRepository;
import com.board.kimicetea.domain.mbti.repository.FunQuestionRepository;
import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminFunQuestionService {

    private static final String TEST_VERSION = "v1.0";

    private final FunQuestionRepository funQuestionRepository;
    private final FunQuestionOptionRepository funQuestionOptionRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /** 전체 FUN 문항 조회 — is_visible 무관, ID 오름차순, options JOIN FETCH */
    @Transactional(readOnly = true)
    public List<AdminFunQuestionResponse> getAllFunQuestions() {
        return funQuestionRepository.findAllWithOptionsByTestVersionOrderByIdAsc(TEST_VERSION)
                .stream()
                .map(AdminFunQuestionResponse::from)
                .toList();
    }

    /**
     * FUN 문항 + 선택지 생성 — testVersion은 항상 "v1.0" 고정, isVisible 기본 true
     * 주의: 문항 저장 직후엔 1차 캐시에 options가 비어있는 상태로 남아있어 detach 후 재조회 필요
     *       (VoteService.createVotePostMeta와 동일한 이유)
     */
    @Transactional
    public AdminFunQuestionResponse createFunQuestion(final AdminFunQuestionCreateRequest request) {
        final FunQuestion question = FunQuestion.builder()
                .title(request.title())
                .testVersion(TEST_VERSION)
                .build();
        funQuestionRepository.save(question);

        final List<FunQuestionOption> options = new ArrayList<>();
        final List<AdminFunOptionCreateRequest> optionRequests = request.options();
        for (int i = 0; i < optionRequests.size(); i++) {
            options.add(FunQuestionOption.builder()
                    .question(question)
                    .optionText(optionRequests.get(i).optionText())
                    .displayOrder((short) i)
                    .build());
        }
        funQuestionOptionRepository.saveAll(options);

        entityManager.flush();
        entityManager.detach(question);
        final FunQuestion saved = funQuestionRepository.findByIdWithOptions(question.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.QUESTION_NOT_FOUND));
        return AdminFunQuestionResponse.from(saved);
    }

    /**
     * FUN 문항 제목 및 선택지 텍스트 수정
     * 주의: 요청의 옵션 ID가 해당 문항에 속하지 않으면 QUESTION_NOT_FOUND 예외
     */
    @Transactional
    public void updateFunQuestion(final Long id, final AdminFunQuestionUpdateRequest request) {
        final FunQuestion question = funQuestionRepository.findByIdWithOptions(id)
                .orElseThrow(() -> new CustomException(ErrorCode.QUESTION_NOT_FOUND));
        question.updateTitle(request.title());

        final Map<Long, FunQuestionOption> optionMap = question.getOptions().stream()
                .collect(Collectors.toMap(FunQuestionOption::getId, Function.identity()));

        for (final AdminFunOptionUpdateRequest optReq : request.options()) {
            final FunQuestionOption option = optionMap.get(optReq.id());
            if (option == null) {
                throw new CustomException(ErrorCode.QUESTION_NOT_FOUND);
            }
            option.updateText(optReq.optionText());
        }
    }

    /** FUN 문항 노출 여부 변경 */
    @Transactional
    public void changeVisibility(final Long id, final AdminFunQuestionVisibilityRequest request) {
        final FunQuestion question = funQuestionRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.QUESTION_NOT_FOUND));
        question.changeVisibility(request.visible());
    }
}
