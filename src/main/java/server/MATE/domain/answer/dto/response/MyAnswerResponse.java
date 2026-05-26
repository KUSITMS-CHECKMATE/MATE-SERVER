package server.MATE.domain.answer.dto.response;

import java.util.List;

public record MyAnswerResponse(
        Integer totalPromotionReward,
        List<MyAnswerItem> answers
) {
    public static MyAnswerResponse of(Integer totalPromotionReward, List<MyAnswerItem> answers) {
        return new MyAnswerResponse(totalPromotionReward, answers);
    }
}
