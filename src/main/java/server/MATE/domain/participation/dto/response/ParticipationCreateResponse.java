package server.MATE.domain.participation.dto.response;

import server.MATE.domain.participation.entity.Participation;

public record ParticipationCreateResponse(Long participationId) {

    public static ParticipationCreateResponse from(Participation participation) {
        return new ParticipationCreateResponse(participation.getId());
    }
}
