package server.MATE.domain.question.dto.response;

import server.MATE.domain.question.entity.FiveSecond;
import server.MATE.domain.question.entity.ImageRatio;

import java.util.List;

public record FiveSecondCreateResponse(
        Long questionId,
        Long fiveSecondId,
        String imageKey,
        ImageRatio imageRatio,
        boolean isObjective,
        Boolean isDuplicate,
        Integer minSelect,
        Integer maxSelect,
        Boolean isOther,
        List<String> options
) {
    public static FiveSecondCreateResponse from(FiveSecond entity) {
        return new FiveSecondCreateResponse(
                entity.getQuestion().getId(),
                entity.getId(),
                entity.getImageKey(),
                entity.getImageRatio(),
                entity.isObjective(),
                entity.getIsDuplicate(),
                entity.getMinSelect(),
                entity.getMaxSelect(),
                entity.getIsOther(),
                entity.getOptions().stream()
                        .map(option -> option.getContent())
                        .toList()
        );
    }
}
