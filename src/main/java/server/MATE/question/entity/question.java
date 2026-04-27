package server.MATE.question.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.MATE.question.enums.QuestionType;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Entity
@Table(name = "question")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "test_id", nullable = false)
    private Long testId;

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
    private cardSorting cardSortingDetail;

    private question(Long testId, QuestionType questionType, String title, String description, Long sequence) {
        this.testId = testId;
        this.questionType = questionType;
        this.title = title;
        this.description = description;
        this.sequence = sequence == null ? 0L : sequence;
    }

    public static question createCardSortingQuestion(Long testId, String title, String description, Long sequence,
                                                     List<String> cards) {
        question question = new question(testId, QuestionType.CARD_SORTING, title, description, sequence);
        question.cardSortingDetail = cardSorting.create(question, cards);
        return question;
    }

}
