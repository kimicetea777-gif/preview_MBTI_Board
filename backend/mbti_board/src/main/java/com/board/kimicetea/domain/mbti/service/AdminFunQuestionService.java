package com.board.kimicetea.domain.mbti.service;

import com.board.kimicetea.domain.mbti.dto.AdminFunOptionUpdateRequest;
import com.board.kimicetea.domain.mbti.dto.AdminFunQuestionResponse;
import com.board.kimicetea.domain.mbti.dto.AdminFunQuestionUpdateRequest;
import com.board.kimicetea.domain.mbti.dto.AdminFunQuestionVisibilityRequest;
import com.board.kimicetea.domain.mbti.entity.FunQuestion;
import com.board.kimicetea.domain.mbti.entity.FunQuestionOption;
import com.board.kimicetea.domain.mbti.repository.FunQuestionRepository;
import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 어드민 FUN 문항 관리 서비스 — 전체 조회·노출 변경 담당
 * 주의: 문항 내용(title·options)은 Flyway 시드로만 관리, 변경 불가
 */
@Service
@RequiredArgsConstructor
public class AdminFunQuestionService {

    private final FunQuestionRepository funQuestionRepository;

    /** 전체 FUN 문항 조회 — is_visible 무관, ID 오름차순, options JOIN FETCH */
    @Transactional(readOnly = true)
    public List<AdminFunQuestionResponse> getAllFunQuestions() {
        return funQuestionRepository.findAllWithOptionsByTestVersionOrderByIdAsc("v1.0")
                .stream()
                .map(AdminFunQuestionResponse::from)
                .toList();
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
