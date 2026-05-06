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
@Table(name = "ab_test")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AbTest extends BaseEntity {

    @Id
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "a_image_key", nullable = false)
    private String aImageKey;

    @Column(name = "b_image_key", nullable = false)
    private String bImageKey;

    @Builder
    public AbTest(Question question, String aImageKey, String bImageKey) {
        this.question = question;
        this.aImageKey = aImageKey;
        this.bImageKey = bImageKey;
    }
}
