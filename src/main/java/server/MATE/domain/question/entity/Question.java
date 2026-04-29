package server.MATE.domain.question.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.MATE.domain.question.enums.QuestionType;
import server.MATE.domain.test.entity.Test;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_id", nullable = false)
    private Test test;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 30)
    private QuestionType questionType;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "sequence", nullable = false)
    private Long sequence = 0L;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    private Question(Test test, QuestionType questionType, String title, String description, Long sequence) {
        this.test = test;
        this.questionType = questionType;
        this.title = title;
        this.description = description;
        this.sequence = sequence == null ? 0L : sequence;
    }

    public static Question create(Test test, QuestionType questionType, String title, String description, Long sequence) {
        return new Question(test, questionType, title, description, sequence);
    }
}
