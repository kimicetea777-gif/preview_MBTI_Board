package com.board.kimicetea.domain.mbti;

import com.board.kimicetea.domain.mbti.entity.MbtiMetadata;
import com.board.kimicetea.domain.mbti.repository.MbtiMetadataRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * NONE MBTI sentinel 값을 애플리케이션 시작 시 한 번만 로드해 캐싱
 * 주의: mbti_metadata 테이블에 NONE 시드 데이터가 없으면 기동 실패
 */
@Component
@RequiredArgsConstructor
public class MbtiMetadataSupport {

    private static final String NONE_MBTI_ID = "NONE";

    private final MbtiMetadataRepository mbtiMetadataRepository;
    private MbtiMetadata noneMbti;

    @PostConstruct
    void init() {
        noneMbti = mbtiMetadataRepository.findById(NONE_MBTI_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "NONE MBTI seed data missing — check mbti_metadata table"));
    }

    public MbtiMetadata getNoneMbti() {
        return noneMbti;
    }
}
