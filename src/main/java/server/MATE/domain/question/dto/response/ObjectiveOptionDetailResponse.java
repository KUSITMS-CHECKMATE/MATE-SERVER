package server.MATE.domain.question.dto.response;

import server.MATE.domain.question.entity.ObjectiveOption;

public record ObjectiveOptionDetailResponse(
        Long objectiveOptionId,
        String content,
        String imageUrl,
        Integer sequence,
        boolean isOtherOption
) {
    public static ObjectiveOptionDetailResponse from(ObjectiveOption option, String imageUrl) {
        return new ObjectiveOptionDetailResponse(
                option.getId(),
                option.getContent(),
                imageUrl,
                option.getSequence(),
                Boolean.TRUE.equals(option.getIsOtherOption())
        );
    }
}
