package server.MATE.domain.question.dto.response;

import server.MATE.domain.question.entity.ObjectiveOption;
import server.MATE.global.storage.dto.ImageResponse;

public record ObjectiveOptionDetailResponse(
        Long objectiveOptionId,
        String content,
        ImageResponse image,
        Integer sequence,
        boolean isOtherOption
) {
    public static ObjectiveOptionDetailResponse from(ObjectiveOption option, ImageResponse image) {
        return new ObjectiveOptionDetailResponse(
                option.getId(),
                option.getContent(),
                image,
                option.getSequence(),
                Boolean.TRUE.equals(option.getIsOtherOption())
        );
    }
}
