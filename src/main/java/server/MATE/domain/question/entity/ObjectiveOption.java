package server.MATE.domain.question.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.MATE.global.common.entity.BaseEntity;

@Getter
@Entity
@Table(name = "objective_option")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ObjectiveOption extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "objective_id", nullable = false)
    private Objective objective;

    @Column(nullable = false, length = 17)
    private String content;

    @Column(length = 255)
    private String imageKey;

    @Column(nullable = false)
    private Integer sequence;

    private Boolean isOtherOption;

    @Builder
    public ObjectiveOption(Objective objective, String content, String imageKey, Integer sequence, Boolean isOtherOption) {
        this.objective = objective;
        this.content = content;
        this.imageKey = imageKey;
        this.sequence = sequence;
        this.isOtherOption = isOtherOption;
    }
}
