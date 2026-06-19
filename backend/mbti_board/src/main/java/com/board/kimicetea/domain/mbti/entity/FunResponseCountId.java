package com.board.kimicetea.domain.mbti.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * fun_response_counts 복합 PK — (option_id, mbti_id)
 */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class FunResponseCountId implements Serializable {

    @Column(name = "option_id")
    private Long optionId;

    @Column(name = "mbti_id", length = 6)
    private String mbtiId;
}
