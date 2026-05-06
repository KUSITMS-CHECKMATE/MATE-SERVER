package server.MATE.domain.question.dto.response;

import server.MATE.domain.question.entity.Objective;

import java.util.List;

public record ObjectiveCreateResponse(
        Long questionId,
        boolean isDuplicate,
        Integer maxSelect,
        Integer minSelect,
        boolean isOther,
        List<ObjectiveOptionResponse> options
) {
    public static ObjectiveCreateResponse from(Objective objective) {
        return new ObjectiveCreateResponse(
                objective.getId(),
                objective.isDuplicate(),
                objective.getMaxSelect(),
                objective.getMinSelect(),
                objective.isOther(),
                objective.getOptions().stream()
                        .map(ObjectiveOptionResponse::from)
                        .toList()
        );
    }
}
