package server.MATE.toss.dto.request;

public record TossPromotionResultRequest(
        Long tossUserKey,
        String promotionCode,
        String key
) {
}
