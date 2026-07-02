package com.board.kimicetea.domain.mbti.service;

import com.board.kimicetea.domain.mbti.dto.FunQuestionResponse;
import com.board.kimicetea.domain.mbti.dto.MbtiQuestionResponse;
import com.board.kimicetea.domain.mbti.dto.MbtiTestHistoryResponse;
import com.board.kimicetea.domain.mbti.dto.MbtiTestResultResponse;
import com.board.kimicetea.domain.mbti.dto.MbtiTestSubmitRequest;
import com.board.kimicetea.domain.mbti.dto.QuestionResponse;
import com.board.kimicetea.domain.mbti.entity.MbtiMetadata;
import com.board.kimicetea.domain.mbti.entity.MbtiQuestion;
import com.board.kimicetea.domain.mbti.entity.MbtiTestHistory;
import com.board.kimicetea.domain.mbti.repository.FunQuestionRepository;
import com.board.kimicetea.domain.mbti.repository.MbtiMetadataRepository;
import com.board.kimicetea.domain.mbti.repository.MbtiQuestionRepository;
import com.board.kimicetea.domain.mbti.repository.MbtiTestHistoryRepository;
import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.enums.MbtiVerified;
import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * MBTI 메타데이터 및 검사 비즈니스 로직
 * 주의: submitTest()만 쓰기 트랜잭션, 나머지는 읽기 전용
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MbtiService {

    private final MbtiMetadataRepository mbtiMetadataRepository;
    private final MbtiQuestionRepository mbtiQuestionRepository;
    private final FunQuestionRepository funQuestionRepository;
    private final MbtiTestHistoryRepository mbtiTestHistoryRepository;
    private final UserRepository userRepository;

    /**
     * MBTI 32문항 + FN 3문항 혼합 반환
     * 주의: FN 질문은 fun_questions 테이블에서 별도 조회 후 셔플하여 섞음
     */
    public List<QuestionResponse> getTestQuestions() {
        final List<MbtiQuestion> raw =
                mbtiQuestionRepository.findRandomQuestionsByVersion("v1.0");

        final Map<String, Long> countByCategory = raw.stream()
                .collect(Collectors.groupingBy(
                        MbtiQuestion::getCategory,
                        Collectors.counting()));

        final List<String> insufficient = List.of("EI", "SN", "TF", "JP").stream()
                .filter(cat -> countByCategory.getOrDefault(cat, 0L) < 8)
                .toList();

        if (!insufficient.isEmpty()) {
            throw new CustomException(ErrorCode.INSUFFICIENT_VISIBLE_QUESTIONS);
        }

        final List<QuestionResponse> questions = new ArrayList<>(
                raw.stream().map(MbtiQuestionResponse::from).toList()
        );

        final List<Long> funIds = funQuestionRepository.findRandomIdsByVersion("v1.0", 3);
        funQuestionRepository.findAllWithOptionsByIds(funIds)
                .stream()
                .map(FunQuestionResponse::from)
                .forEach(questions::add);

        Collections.shuffle(questions);
        return questions;
    }

    /**
     * 검사 결과 제출 — 점수로 MBTI 재도출 후 이력 저장 및 유저 MBTI 업데이트
     * 주의: 클라이언트가 보낸 점수만 신뢰하고 MBTI 문자열은 서버가 독립적으로 판정
     */
    @Transactional
    public MbtiTestResultResponse submitTest(final UUID userId, final MbtiTestSubmitRequest request) {
        final User user = userRepository.findWithMbtiById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        final String mbtiId = deriveMbtiId(request.eiScore(), request.snScore(), request.tfScore(), request.jpScore());

        final MbtiMetadata mbti = mbtiMetadataRepository.findById(mbtiId)
                .orElseThrow(() -> new CustomException(ErrorCode.MBTI_NOT_FOUND));

        mbtiTestHistoryRepository.save(MbtiTestHistory.builder()
                .user(user)
                .mbti(mbti)
                .eiScore(request.eiScore().shortValue())
                .snScore(request.snScore().shortValue())
                .tfScore(request.tfScore().shortValue())
                .jpScore(request.jpScore().shortValue())
                .build());

        user.changeMbti(mbti, MbtiVerified.TESTED);

        return new MbtiTestResultResponse(mbtiId, request.eiScore(), request.snScore(), request.tfScore(),
                request.jpScore());
    }

    /** 내 검사 이력 반환 — 최신순 */
    public List<MbtiTestHistoryResponse> getMyMbtiHistory(final UUID userId) {
        return mbtiTestHistoryRepository.findLatestByUserId(userId, Pageable.unpaged())
                .stream()
                .map(MbtiTestHistoryResponse::from)
                .toList();
    }

    private String deriveMbtiId(final short eiScore, final short snScore, final short tfScore, final short jpScore) {
        return String.valueOf(new char[]{
                eiScore >= 0 ? 'E' : 'I',
                snScore >= 0 ? 'S' : 'N',
                tfScore >= 0 ? 'T' : 'F',
                jpScore >= 0 ? 'J' : 'P'
        });
    }
}
