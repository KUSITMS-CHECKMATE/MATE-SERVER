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
        List<FiveSecondOptionResponse> options
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
                        .map(FiveSecondOptionResponse::from)
                        .toList()
        );
    }

    public record FiveSecondOptionResponse(
            Long fiveSecondOptionId,
            String content,
            Integer sequence,
            boolean isOtherOption
    ) {
        public static FiveSecondOptionResponse from(server.MATE.domain.question.entity.FiveSecondOption option) {
            return new FiveSecondOptionResponse(
                    option.getId(),
                    option.getContent(),
                    option.getSequence(),
                    Boolean.TRUE.equals(option.getIsOtherOption())
            );
        }
    }
}
