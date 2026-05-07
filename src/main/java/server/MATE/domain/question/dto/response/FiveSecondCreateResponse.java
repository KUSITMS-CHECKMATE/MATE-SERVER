package server.MATE.domain.question.dto.response;

import server.MATE.domain.question.entity.FiveSecond;

import java.util.List;

public record FiveSecondCreateResponse(
        Long questionId,
        String imageKey,
        boolean isObjective,
        Boolean isDuplicate,
        Integer minSelect,
        Integer maxSelect,
        List<FiveSecondOptionResponse> options
) {
    public static FiveSecondCreateResponse from(FiveSecond fiveSecond) {
        return new FiveSecondCreateResponse(
                fiveSecond.getId(),
                fiveSecond.getImageKey(),
                fiveSecond.isObjective(),
                fiveSecond.getIsDuplicate(),
                fiveSecond.getMinSelect(),
                fiveSecond.getMaxSelect(),
                fiveSecond.getOptions().stream()
                        .map(FiveSecondOptionResponse::from)
                        .toList()
        );
    }
}
