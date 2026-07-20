package server.MATE.global.discord.bot.message;

import java.time.format.DateTimeFormatter;

import server.MATE.domain.test.dto.response.AdminTestDetailResponse;
import server.MATE.domain.test.dto.response.AdminTestListItemResponse;
import server.MATE.domain.test.dto.response.AdminTestListResponse;
import server.MATE.domain.test.dto.response.AdminTestStatusResponse;

public final class AdminBotMessageFormatter {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private AdminBotMessageFormatter() {
    }

    public static String formatList(AdminTestListResponse response) {
        if (response.tests().isEmpty()) {
            return String.format("조회된 테스트가 없습니다. (page: %d, size: %d, total: %d)",
                    response.page(), response.size(), response.totalCount());
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("**테스트 목록** (page: %d, size: %d, total: %d)\n",
                response.page(), response.size(), response.totalCount()));
        for (AdminTestListItemResponse item : response.tests()) {
            sb.append(String.format("`#%d` %s | %dP | `%s` | %s\n",
                    item.testId(),
                    item.title(),
                    item.reward(),
                    item.testStatus(),
                    item.createdAt().format(TIMESTAMP_FORMAT)));
        }
        return sb.toString();
    }

    public static String formatDetail(AdminTestDetailResponse response) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("**#%d %s**\n", response.testId(), response.title()));
        sb.append(String.format("상태: `%s`\n", response.testStatus()));
        sb.append(String.format("리워드: %dP\n", response.reward()));
        sb.append(String.format("카테고리: %s\n", String.join(", ", response.categories())));
        sb.append(String.format("생성 시각: `%s`\n", response.createdAt().format(TIMESTAMP_FORMAT)));
        if (response.rejectionReason() != null && !response.rejectionReason().isBlank()) {
            sb.append(String.format("반려 사유: %s\n", response.rejectionReason()));
        }
        sb.append(String.format("설명: %s", response.description()));
        return sb.toString();
    }

    public static String formatApprove(AdminTestStatusResponse response) {
        return String.format("테스트 `#%d`를 승인했습니다. (상태: `%s`)", response.testId(), response.testStatus());
    }

    public static String formatReject(AdminTestStatusResponse response) {
        return String.format("테스트 `#%d`를 반려했습니다. (상태: `%s`)", response.testId(), response.testStatus());
    }
}
