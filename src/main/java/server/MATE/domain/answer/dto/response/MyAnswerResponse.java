package server.MATE.domain.answer.dto.response;

import java.util.List;

public record MyAnswerResponse(
        Long totalPromotionReward,
        List<MyAnswerItem> answers
) {
    public static MyAnswerResponse of(Long totalPromotionReward, List<MyAnswerItem> answers) {
        return new MyAnswerResponse(totalPromotionReward, answers);
    }
}
