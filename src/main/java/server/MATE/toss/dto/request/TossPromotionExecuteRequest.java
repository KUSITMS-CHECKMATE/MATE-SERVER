package server.MATE.toss.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TossPromotionExecuteRequest(
        Long tossUserKey,
        String promotionCode,
        String key,
        Integer amount
) {
}
