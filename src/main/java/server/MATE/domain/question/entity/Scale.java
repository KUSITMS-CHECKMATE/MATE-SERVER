package server.MATE.domain.question.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.MATE.global.common.entity.BaseEntity;

@Getter
@Entity
@Table(name = "scale")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Scale extends BaseEntity {

    private static final String DEFAULT_MIN_LABEL = "전혀 아니다";
    private static final String DEFAULT_MAX_LABEL = "매우 그렇다";

    @Id
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column
    private String imageKey;

    @Column(length = 100)
    private String minLabel;

    @Column(length = 100)
    private String maxLabel;

    @Column(name = "scale_range", nullable = false)
    private Integer range;

    @Builder
    public Scale(Question question, String imageKey, String minLabel, String maxLabel, Integer range) {
        this.question = question;
        this.imageKey = imageKey;
        this.minLabel = resolveLabel(minLabel, DEFAULT_MIN_LABEL);
        this.maxLabel = resolveLabel(maxLabel, DEFAULT_MAX_LABEL);
        this.range = range == null ? 5 : range;
    }

    private String resolveLabel(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
