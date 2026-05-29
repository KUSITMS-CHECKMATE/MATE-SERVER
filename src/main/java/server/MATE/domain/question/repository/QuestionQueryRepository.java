package server.MATE.domain.question.repository;

import server.MATE.domain.question.dto.response.AnswerQuestionTypeView;
import server.MATE.domain.question.dto.response.QuestionSummaryItem;
import server.MATE.domain.question.entity.Question;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface QuestionQueryRepository {

    Optional<Question> findQuestionByIdInTest(Long questionId, Long testId);

    long countQuestionsInTest(Long testId);

    List<Question> findQuestionsInTest(Long testId);

    List<Question> findQuestionsInTestIncludingDeleted(Long testId);

    List<AnswerQuestionTypeView> findAnswerQuestionTypesInTest(Long testId);

    List<QuestionSummaryItem> findQuestionSummariesInTest(Long testId);

    long softDeleteByTestId(Long testId, LocalDateTime deletedAt);
}
