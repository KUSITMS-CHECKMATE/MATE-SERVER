package server.MATE.domain.question.dto.response;

import server.MATE.domain.question.entity.Subjective;

public record SubjectiveCreateResponse(
        Long questionId,
        String imageKey
) {
    public static SubjectiveCreateResponse from(Subjective subjective) {
        return new SubjectiveCreateResponse(
                subjective.getId(),
                subjective.getImageKey()
        );
    }
}
