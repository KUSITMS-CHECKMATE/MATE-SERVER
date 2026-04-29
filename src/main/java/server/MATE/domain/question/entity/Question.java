package server.MATE.domain.question.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.MATE.domain.cardsorting.entity.CardSorting;
import server.MATE.domain.question.enums.QuestionType;
import server.MATE.domain.test.entity.Test;

import java.time.LocalDateTime;
import java.util.List;

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

    @OneToOne(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private CardSorting cardSortingDetail;

    private Question(Test test, QuestionType questionType, String title, String description, Long sequence) {
        this.test = test;
        this.questionType = questionType;
        this.title = title;
        this.description = description;
        this.sequence = sequence == null ? 0L : sequence;
    }

    public static Question createCardSortingQuestion(Test test, String title, String description, Long sequence,
                                                     List<String> cards) {
        Question question = new Question(test, QuestionType.CARD_SORTING, title, description, sequence);
        question.cardSortingDetail = CardSorting.create(question, cards);
        return question;
    }

    public static Question createTreeTestQuestion(Test test, String title, String description, Long sequence) {
        return new Question(test, QuestionType.TREE_TEST, title, description, sequence);
    }
}
