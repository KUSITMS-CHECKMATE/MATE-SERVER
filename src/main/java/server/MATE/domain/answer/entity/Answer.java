package server.MATE.domain.answer.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.global.common.entity.BaseEntity;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Entity
@Table(name = "answer", uniqueConstraints = @UniqueConstraint(columnNames = {"participation_id", "question_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Answer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long participationId;

    @Column(nullable = false, updatable = false)
    private Long questionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private QuestionType questionType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false, columnDefinition = "jsonb")
    private Map<String, Object> answer;

    private LocalDateTime deletedAt;

    @Builder
    public Answer(Long participationId, Long questionId, QuestionType questionType, Map<String, Object> answer) {
        this.participationId = participationId;
        this.questionId = questionId;
        this.questionType = questionType;
        this.answer = answer;
    }

    public void delete(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
