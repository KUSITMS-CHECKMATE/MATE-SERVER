package server.MATE.domain.question.dto.response;

import server.MATE.domain.question.entity.FiveSecondOption;

public record FiveSecondOptionResponse(
        Long id,
        String content,
        Integer sequence
) {
    public static FiveSecondOptionResponse from(FiveSecondOption option) {
        return new FiveSecondOptionResponse(
                option.getId(),
                option.getContent(),
                option.getSequence()
        );
    }
}
