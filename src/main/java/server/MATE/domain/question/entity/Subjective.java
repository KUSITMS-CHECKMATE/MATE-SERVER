package server.MATE.domain.question.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "subjective")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subjective {

    @Id
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    @Column(length = 255)
    private String imageKey;

    @Builder
    public Subjective(Question question, String imageKey) {
        this.question = question;
        this.imageKey = imageKey;
    }
}
