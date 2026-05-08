package server.MATE.domain.question.dto.response;

import server.MATE.domain.question.entity.Objective;

import java.util.List;

public record ObjectiveCreateResponse(
        Long questionId,
        Long objectiveId,
        boolean isDuplicate,
        Integer maxSelect,
        Integer minSelect,
        boolean isOther,
        List<ObjectiveOptionResponse> options
) {
    public static ObjectiveCreateResponse from(Objective entity) {
        return new ObjectiveCreateResponse(
                entity.getQuestion().getId(),
                entity.getId(),
                entity.isDuplicate(),
                entity.getMaxSelect(),
                entity.getMinSelect(),
                entity.isOther(),
                entity.getOptions().stream()
                        .map(option -> new ObjectiveOptionResponse(
                                option.getId(),
                                option.getContent(),
                                option.getImageKey(),
                                option.getSequence()
                        ))
                        .toList()
        );
    }

    public record ObjectiveOptionResponse(
            Long id,
            String content,
            String imageKey,
            Integer sequence
    ) {
    }
}
