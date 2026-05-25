package server.MATE.domain.question.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import server.MATE.global.common.entity.BaseEntity;

@Getter
@Entity
@Table(name = "objective")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Objective extends BaseEntity {

    @Id
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    @Column(nullable = false)
    private boolean isDuplicate;

    private Integer maxSelect;
    private Integer minSelect;

    @Column(nullable = false)
    private boolean isOther;

    @OneToMany(mappedBy = "objective", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC")
    private List<ObjectiveOption> options = new ArrayList<>();

    @Builder
    public Objective(Question question, boolean isDuplicate, Integer maxSelect, Integer minSelect, boolean isOther) {
        this.question = question;
        this.isDuplicate = isDuplicate;
        this.maxSelect = maxSelect;
        this.minSelect = minSelect;
        this.isOther = isOther;
    }

    public void addOption(ObjectiveOption option) {
        options.add(option);
    }
}
