package server.MATE.domain.question.dto.response;

import server.MATE.domain.question.entity.Subjective;

public record SubjectiveQuestionCreateResponse(
        Long questionId,
        String imageKey
) {
    public static SubjectiveQuestionCreateResponse from(Subjective subjective) {
        return new SubjectiveQuestionCreateResponse(
                subjective.getId(),
                subjective.getImageKey()
        );
    }
}
