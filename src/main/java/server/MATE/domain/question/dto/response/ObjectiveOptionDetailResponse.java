package server.MATE.domain.question.dto.response;

import server.MATE.domain.question.entity.ObjectiveOption;

public record ObjectiveOptionDetailResponse(
        Long objectiveOptionId,
        String content,
        String imageKey,
        Integer sequence,
        boolean isOtherOption
) {
    public static ObjectiveOptionDetailResponse from(ObjectiveOption option) {
        return new ObjectiveOptionDetailResponse(
                option.getId(),
                option.getContent(),
                option.getImageKey(),
                option.getSequence(),
                Boolean.TRUE.equals(option.getIsOtherOption())
        );
    }
}
