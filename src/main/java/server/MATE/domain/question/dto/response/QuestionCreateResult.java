package server.MATE.domain.question.dto.response;

import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;

public record QuestionCreateResult(
        Long questionId,
        QuestionType type,
        Long sequence,
        String title
) {
    public static QuestionCreateResult from(Question question) {
        return new QuestionCreateResult(
                question.getId(),
                question.getQuestionType(),
                question.getSequence(),
                question.getTitle()
        );
    }
}
