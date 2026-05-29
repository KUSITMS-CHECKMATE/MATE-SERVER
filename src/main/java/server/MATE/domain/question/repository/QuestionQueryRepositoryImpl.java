package server.MATE.domain.question.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import server.MATE.domain.question.dto.response.AnswerQuestionTypeView;
import server.MATE.domain.question.dto.response.QuestionSummaryItem;
import server.MATE.domain.question.entity.Question;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import static server.MATE.domain.question.entity.QQuestion.question;

@Repository
@RequiredArgsConstructor
public class QuestionQueryRepositoryImpl implements QuestionQueryRepository {

    private final JPAQueryFactory queryFactory;

    // 삭제되지 않은 질문만 가져오는 조건
    private static BooleanExpression notDeleted() {
        return question.deletedAt.isNull();
    }

    // 전달받은 ID와 질문 ID가 같은 경우만 거르는 조건
    private static BooleanExpression idEq(Long questionId) {
        return question.id.eq(questionId);
    }

    // 전달받은 테스트 ID와 질문의 테스트 ID가 같은 경우만 거르는 조건
    private static BooleanExpression testIdEq(Long testId) {
        return question.testId.eq(testId);
    }

    @Override
    public Optional<Question> findQuestionByIdInTest(Long questionId, Long testId) {
        if (questionId == null || testId == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                queryFactory
                        .selectFrom(question)
                        .where(idEq(questionId), testIdEq(testId), notDeleted())
                        .fetchOne()
        );
    }

    @Override
    public long countQuestionsInTest(Long testId) {
        if (testId == null) {
            return 0L;
        }

        Long count = queryFactory
                .select(question.count())
                .from(question)
                .where(testIdEq(testId), notDeleted())
                .fetchOne();

        return count == null ? 0L : count;
    }

    @Override
    public List<Question> findQuestionsInTest(Long testId) {
        if (testId == null) {
            return List.of();
        }

        return queryFactory
                .selectFrom(question)
                .where(testIdEq(testId), notDeleted())
                .orderBy(question.sequence.asc())
                .fetch();
    }

    @Override
    public List<Question> findQuestionsInTestIncludingDeleted(Long testId) {
        if (testId == null) {
            return List.of();
        }

        return queryFactory
                .selectFrom(question)
                .where(testIdEq(testId))
                .orderBy(question.sequence.asc())
                .fetch();
    }

    @Override
    public List<AnswerQuestionTypeView> findAnswerQuestionTypesInTest(Long testId) {
        if (testId == null) {
            return List.of();
        }

        return queryFactory
                .select(Projections.constructor(
                        AnswerQuestionTypeView.class,
                        question.id,
                        question.questionType
                ))
                .from(question)
                .where(testIdEq(testId), notDeleted())
                .fetch();
    }

    @Override
    public List<QuestionSummaryItem> findQuestionSummariesInTest(Long testId) {
        if (testId == null) {
            return List.of();
        }

        return queryFactory
                .select(Projections.constructor(
                        QuestionSummaryItem.class,
                        question.id,
                        question.sequence,
                        question.title,
                        question.questionType
                ))
                .from(question)
                .where(testIdEq(testId), notDeleted())
                .orderBy(question.sequence.asc())
                .fetch();
    }

    @Override
    public long softDeleteByTestId(Long testId, LocalDateTime deletedAt) {
        if (testId == null || deletedAt == null) {
            return 0L;
        }

        return queryFactory
                .update(question)
                .set(question.deletedAt, deletedAt)
                .where(testIdEq(testId), notDeleted())
                .execute();
    }
}
