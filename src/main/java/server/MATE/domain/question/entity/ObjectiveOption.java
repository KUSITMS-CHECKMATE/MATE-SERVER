package server.MATE.domain.question.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "objective_option")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ObjectiveOption {

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

    @Builder
    public ObjectiveOption(Objective objective, String content, String imageKey, Integer sequence) {
        this.objective = objective;
        this.content = content;
        this.imageKey = imageKey;
        this.sequence = sequence;
    }
}
