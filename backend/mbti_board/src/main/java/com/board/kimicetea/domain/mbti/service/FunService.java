package com.board.kimicetea.domain.mbti.service;

import com.board.kimicetea.domain.mbti.dto.FunResponseSubmitRequest;
import com.board.kimicetea.domain.mbti.dto.FunStatRow;
import com.board.kimicetea.domain.mbti.dto.FunStatsResponse;
import com.board.kimicetea.domain.mbti.entity.FunQuestion;
import com.board.kimicetea.domain.mbti.repository.FunQuestionRepository;
import com.board.kimicetea.domain.mbti.repository.FunResponseCountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * FN(재미) 질문 응답 저장·통계 비즈니스 로직
 * 주의: submitResponses()만 쓰기 트랜잭션, 나머지 읽기 전용
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FunService {

    private final FunQuestionRepository funQuestionRepository;
    private final FunResponseCountRepository funResponseCountRepository;

    /** DB에서 랜덤 3개 ID 조회 후 options JOIN FETCH */
    public List<FunQuestion> getRandomFunQuestions() {
        final List<Long> randomIds = funQuestionRepository.findRandomIdsByVersion("v1.0", 3);
        return funQuestionRepository.findAllWithOptionsByIds(randomIds);
    }

    /** optionId·mbtiId 조합 카운트 업서트 */
    @Transactional
    public void submitResponses(final FunResponseSubmitRequest request) {
        for (final Long optionId : request.optionIds()) {
            funResponseCountRepository.upsertCount(optionId, request.mbtiId());
        }
    }

    /** 전체 FN 질문 통계 반환 — 1회 쿼리로 집계 */
    public List<FunStatsResponse> getAllStats() {
        return assembleFunStats(funQuestionRepository.findAllStats("v1.0"));
    }

    private List<FunStatsResponse> assembleFunStats(final List<FunStatRow> rows) {
        final Map<Long, List<FunStatRow>> byQuestion = new LinkedHashMap<>();
        for (final FunStatRow row : rows) {
            byQuestion.computeIfAbsent(row.getQuestionId(), k -> new ArrayList<>()).add(row);
        }

        return byQuestion.entrySet().stream().map(qEntry -> {
            final List<FunStatRow> qRows = qEntry.getValue();
            final String title = qRows.get(0).getQuestionTitle();

            final Map<Long, List<FunStatRow>> byOption = new LinkedHashMap<>();
            for (final FunStatRow row : qRows) {
                byOption.computeIfAbsent(row.getOptionId(), k -> new ArrayList<>()).add(row);
            }

            final List<FunStatsResponse.OptionStats> optionStats = byOption.entrySet().stream()
                    .map(oEntry -> {
                        final List<FunStatRow> oRows = oEntry.getValue();
                        final List<FunStatsResponse.MbtiCount> mbtiCounts = oRows.stream()
                                .filter(r -> r.getMbtiId() != null)
                                .map(r -> new FunStatsResponse.MbtiCount(r.getMbtiId(), r.getCount()))
                                .toList();
                        final long total = mbtiCounts.stream().mapToLong(FunStatsResponse.MbtiCount::count).sum();
                        return new FunStatsResponse.OptionStats(
                                oEntry.getKey(),
                                oRows.get(0).getOptionText(),
                                oRows.get(0).getDisplayOrder(),
                                total,
                                mbtiCounts
                        );
                    })
                    .toList();

            return new FunStatsResponse(qEntry.getKey(), title, optionStats);
        }).toList();
    }
}
