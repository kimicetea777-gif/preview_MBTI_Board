package com.board.kimicetea.domain.mbti.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "mbti_metadata")
public class MbtiMetadata {

    @Id
    @Column(name = "id", length = 6)
    private String id; // INTP, INFP-A

    @Column(name = "icon_url")
    private String iconUrl;

    @Column(name = "color_code", nullable = false, length = 7)
    private String colorCode; // #4A90E2 (Hex Code)

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** MBTI 미설정 항목 여부 — MbtiVerified.DEFAULT 결정에 사용 */
    public boolean isNone() {
        return "NONE".equals(this.id);
    }
}
