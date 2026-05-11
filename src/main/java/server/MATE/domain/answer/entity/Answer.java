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

@Getter
@Entity
@Table(name = "answer")
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
    @Column(nullable = false, columnDefinition = "jsonb")
    private String answer;

    private LocalDateTime deletedAt;

    @Builder
    public Answer(Long participationId, Long questionId, QuestionType questionType, String answer) {
        this.participationId = participationId;
        this.questionId = questionId;
        this.questionType = questionType;
        this.answer = answer;
    }
}
