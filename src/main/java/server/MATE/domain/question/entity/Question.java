package server.MATE.domain.question.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.MATE.global.common.entity.BaseEntity;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "question",
        uniqueConstraints = @UniqueConstraint(columnNames = {"test_id", "sequence"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long testId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionType questionType;

    @Column(nullable = false, length = 34)
    private String title;

    @Column(length = 55)
    private String description;

    @Column(nullable = false)
    private Long sequence;

    private LocalDateTime deletedAt;

    @Builder
    public Question(Long testId, QuestionType questionType, String title, String description, Long sequence) {
        this.testId = testId;
        this.questionType = questionType;
        this.title = title;
        this.description = description;
        this.sequence = sequence;
    }
}
