package server.MATE.domain.question.dto.response;

import server.MATE.domain.question.entity.Subjective;

public record SubjectiveCreateResponse(
        Long questionId,
        Long subjectiveId,
        String imageKey
) {
    public static SubjectiveCreateResponse from(Subjective entity) {
        return new SubjectiveCreateResponse(
                entity.getQuestion().getId(),
                entity.getId(),
                entity.getImageKey()
        );
    }
}
