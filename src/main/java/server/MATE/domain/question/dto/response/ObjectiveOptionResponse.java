package server.MATE.domain.question.dto.response;

import server.MATE.domain.question.entity.ObjectiveOption;

public record ObjectiveOptionResponse(
        Long id,
        String content,
        String imageKey,
        Integer sequence
) {
    public static ObjectiveOptionResponse from(ObjectiveOption option) {
        return new ObjectiveOptionResponse(
                option.getId(),
                option.getContent(),
                option.getImageKey(),
                option.getSequence()
        );
    }
}
