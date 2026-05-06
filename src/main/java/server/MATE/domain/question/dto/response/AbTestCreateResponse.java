package server.MATE.domain.question.dto.response;

import server.MATE.domain.question.entity.AbTest;

public record AbTestCreateResponse(
        Long questionId,
        String aImageKey,
        String bImageKey
) {
    public static AbTestCreateResponse from(AbTest abTest) {
        return new AbTestCreateResponse(
                abTest.getId(),
                abTest.getAImageKey(),
                abTest.getBImageKey()
        );
    }
}
