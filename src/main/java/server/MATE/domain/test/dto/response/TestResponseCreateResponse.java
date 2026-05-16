package server.MATE.domain.test.dto.response;

import server.MATE.domain.participation.entity.Participation;

public record TestResponseCreateResponse(Long participationId) {

    public static TestResponseCreateResponse from(Participation participation) {
        return new TestResponseCreateResponse(participation.getId());
    }
}
