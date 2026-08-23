package server.MATE.domain.question.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.MATE.global.common.entity.BaseEntity;

@Getter
@Entity
@Table(name = "five_second_option")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FiveSecondOption extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "five_second_id", nullable = false)
    private FiveSecond fiveSecond;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private Integer sequence;

    private Boolean isOtherOption;

    @Builder
    public FiveSecondOption(FiveSecond fiveSecond, String content, Integer sequence, Boolean isOtherOption) {
        this.fiveSecond = fiveSecond;
        this.content = content;
        this.sequence = sequence;
        this.isOtherOption = isOtherOption;
    }
}
