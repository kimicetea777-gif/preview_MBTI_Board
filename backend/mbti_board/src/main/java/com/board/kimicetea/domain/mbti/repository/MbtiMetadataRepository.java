package com.board.kimicetea.domain.mbti.repository;

import com.board.kimicetea.domain.mbti.entity.MbtiMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MbtiMetadataRepository extends JpaRepository<MbtiMetadata, String> {
}
