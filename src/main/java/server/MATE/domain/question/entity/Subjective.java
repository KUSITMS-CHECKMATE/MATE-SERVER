package server.MATE.domain.question.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.MATE.global.common.entity.BaseEntity;

@Getter
@Entity
@Table(name = "subjective")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subjective extends BaseEntity {

    @Id
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    @Column
    private String imageKey;

    @Builder
    public Subjective(Question question, String imageKey) {
        this.question = question;
        this.imageKey = imageKey;
    }
}
