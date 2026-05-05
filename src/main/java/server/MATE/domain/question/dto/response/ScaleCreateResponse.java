package server.MATE.domain.question.dto.response;

import server.MATE.domain.question.entity.Scale;

public record ScaleCreateResponse(
        Long questionId,
        String imageKey,
        String minLabel,
        String maxLabel,
        Integer range
) {
    public static ScaleCreateResponse from(Scale scale) {
        return new ScaleCreateResponse(
                scale.getId(),
                scale.getImageKey(),
                scale.getMinLabel(),
                scale.getMaxLabel(),
                scale.getRange()
        );
    }
}
