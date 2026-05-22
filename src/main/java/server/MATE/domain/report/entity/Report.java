package server.MATE.domain.report.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import server.MATE.domain.question.entity.QuestionType;

import java.util.Map;

@Getter
@Entity
@Table(name = "report",
        uniqueConstraints = @UniqueConstraint(columnNames = {"test_id", "question_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long testId;

    @Column(nullable = false)
    private Long questionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionType questionType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> result;

    @Builder
    public Report(Long testId, Long questionId, QuestionType questionType, Map<String, Object> result) {
        this.testId = testId;
        this.questionId = questionId;
        this.questionType = questionType;
        this.result = result;
    }
}
