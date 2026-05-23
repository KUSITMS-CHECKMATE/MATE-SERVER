package server.MATE.domain.question.dto.response;

import server.MATE.domain.question.entity.QuestionType;

public record AnswerQuestionTypeView(
        Long questionId,
        QuestionType questionType
) {
}
