package com.board.kimicetea.domain.mbti.service;

import com.board.kimicetea.domain.mbti.dto.MbtiResponse;
import com.board.kimicetea.domain.mbti.repository.MbtiMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * MBTI 메타데이터 비즈니스 로직
 * 주의: mbti_metadata는 정적 데이터이므로 쓰기 기능 없음
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MbtiService {

    private final MbtiMetadataRepository mbtiMetadataRepository;

    /** 전체 MBTI 목록 반환 */
    @Cacheable("mbtiList")
    public List<MbtiResponse> getAllMbti() {
        return mbtiMetadataRepository.findAll().stream()
                .map(MbtiResponse::from)
                .toList();
    }
}
