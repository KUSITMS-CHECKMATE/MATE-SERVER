package server.MATE.domain.question.dto.response;

import server.MATE.domain.question.entity.FiveSecondOption;

public record FiveSecondOptionDetailResponse(
        Long fiveSecondOptionId,
        String content,
        Integer sequence
) {
    public static FiveSecondOptionDetailResponse from(FiveSecondOption option) {
        return new FiveSecondOptionDetailResponse(option.getId(), option.getContent(), option.getSequence());
    }
}
