package server.MATE.domain.question.dto.response;

import server.MATE.domain.question.entity.FiveSecond;

import java.util.List;

public record FiveSecondCreateResponse(
        Long questionId,
        Long fiveSecondId,
        String imageKey,
        boolean isObjective,
        Boolean isDuplicate,
        Integer minSelect,
        Integer maxSelect,
        List<String> options
) {
    public static FiveSecondCreateResponse from(FiveSecond entity) {
        return new FiveSecondCreateResponse(
                entity.getQuestion().getId(),
                entity.getId(),
                entity.getImageKey(),
                entity.isObjective(),
                entity.getIsDuplicate(),
                entity.getMinSelect(),
                entity.getMaxSelect(),
                entity.getOptions().stream()
                        .map(option -> option.getContent())
                        .toList()
        );
    }
}
