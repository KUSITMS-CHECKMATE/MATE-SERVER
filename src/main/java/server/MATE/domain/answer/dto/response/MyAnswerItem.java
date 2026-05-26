package server.MATE.domain.answer.dto.response;

import server.MATE.global.common.util.DateFormatUtil;

public record MyAnswerItem(
        Long testId,
        String testName,
        String createdAt,
        Integer reward
) {
    public static MyAnswerItem from(MyAnswerItemView view) {
        return new MyAnswerItem(
                view.testId(),
                view.testName(),
                DateFormatUtil.format(view.createdAt()),
                view.reward()
        );
    }
}
