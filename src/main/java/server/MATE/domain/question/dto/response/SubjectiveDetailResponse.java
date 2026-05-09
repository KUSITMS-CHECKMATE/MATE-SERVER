package server.MATE.domain.question.dto.response;

import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.entity.Subjective;

public record SubjectiveDetailResponse(
        Long questionId,
        Long subjectiveId,
        QuestionType type,
        Long sequence,
        String title,
        String description,
        String imageKey
) implements QuestionDetailItem {

    public static SubjectiveDetailResponse of(Question question, Subjective subjective) {
        return new SubjectiveDetailResponse(
                question.getId(),
                subjective.getId(),
                question.getQuestionType(),
                question.getSequence(),
                question.getTitle(),
                question.getDescription(),
                subjective.getImageKey()
        );
    }
}
