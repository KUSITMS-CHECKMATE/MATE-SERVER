package server.MATE.domain.answer.dto.response;

import server.MATE.domain.participation.entity.Participation;

public record AnswerBatchCreateResponse(Long participationId) {

    public static AnswerBatchCreateResponse from(Participation participation) {
        return new AnswerBatchCreateResponse(participation.getId());
    }
}
