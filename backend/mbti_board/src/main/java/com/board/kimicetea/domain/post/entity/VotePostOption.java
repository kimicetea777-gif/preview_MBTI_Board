package com.board.kimicetea.domain.post.entity;

import com.board.kimicetea.domain.mbti.entity.MbtiMetadata;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "vote_post_options")
public class VotePostOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "option_text", length = 100)
    private String optionText;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Short displayOrder = 0;

    // DB 트리거가 관리 — JPA 직접 수정 금지
    @Builder.Default
    @Column(name = "vote_count", nullable = false, insertable = false, updatable = false)
    private Integer voteCount = 0;

    /*-----------------------------------연관관계-------------------------------------------*/

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private VotePost votePost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mbti_target")
    private MbtiMetadata mbtiTarget;
}
