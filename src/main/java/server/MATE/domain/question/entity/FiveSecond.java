package server.MATE.domain.question.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "five_second")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FiveSecond {

    @Id
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    @Column(nullable = false, length = 255)
    private String imageKey;

    @Column(nullable = false)
    private boolean isObjective;

    private Boolean isDuplicate;
    private Integer minSelect;
    private Integer maxSelect;

    private Boolean isOther;

    @OneToMany(mappedBy = "fiveSecond", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC")
    private List<FiveSecondOption> options = new ArrayList<>();

    @Builder
    public FiveSecond(Question question, String imageKey, boolean isObjective,
                      Boolean isDuplicate, Integer minSelect, Integer maxSelect, Boolean isOther) {
        this.question = question;
        this.imageKey = imageKey;
        this.isObjective = isObjective;
        this.isDuplicate = isDuplicate;
        this.minSelect = minSelect;
        this.maxSelect = maxSelect;
        this.isOther = isOther;
    }

    public void addOption(FiveSecondOption option) {
        options.add(option);
    }
}
