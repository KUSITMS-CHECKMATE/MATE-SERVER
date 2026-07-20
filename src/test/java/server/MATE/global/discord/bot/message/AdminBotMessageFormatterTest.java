package server.MATE.global.discord.bot.message;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import server.MATE.domain.test.dto.response.AdminTestDetailResponse;
import server.MATE.domain.test.dto.response.AdminTestListItemResponse;
import server.MATE.domain.test.dto.response.AdminTestListResponse;
import server.MATE.domain.test.dto.response.AdminTestStatusResponse;
import server.MATE.domain.test.entity.TestStatus;

class AdminBotMessageFormatterTest {

    @Test
    @DisplayName("목록이 비어있으면 안내 메시지를 반환한다")
    void formatList_empty() {
        AdminTestListResponse response = new AdminTestListResponse(1, 5, 0, List.of());

        String message = AdminBotMessageFormatter.formatList(response);

        assertThat(message).contains("조회된 테스트가 없습니다");
    }

    @Test
    @DisplayName("목록에 각 테스트의 id, title, reward, status, createdAt이 포함된다")
    void formatList_containsItemFields() {
        AdminTestListItemResponse item = new AdminTestListItemResponse(
                1L, "제목", 200, TestStatus.WAITING, LocalDateTime.of(2026, 7, 20, 10, 0));
        AdminTestListResponse response = new AdminTestListResponse(1, 5, 1, List.of(item));

        String message = AdminBotMessageFormatter.formatList(response);

        assertThat(message)
                .contains("1")
                .contains("제목")
                .contains("200")
                .contains("WAITING")
                .contains("2026-07-20 10:00:00");
    }

    @Test
    @DisplayName("상세 메시지는 반려 사유가 없으면 포함하지 않는다")
    void formatDetail_withoutRejectionReason() {
        AdminTestDetailResponse response = new AdminTestDetailResponse(
                1L, "제목", "설명", 200, List.of("BEAUTY"), List.of(),
                TestStatus.WAITING, null, LocalDateTime.of(2026, 7, 20, 10, 0));

        String message = AdminBotMessageFormatter.formatDetail(response);

        assertThat(message).doesNotContain("반려 사유");
    }

    @Test
    @DisplayName("상세 메시지는 반려 사유가 있으면 포함한다")
    void formatDetail_withRejectionReason() {
        AdminTestDetailResponse response = new AdminTestDetailResponse(
                1L, "제목", "설명", 200, List.of("BEAUTY"), List.of(),
                TestStatus.REJECTED, "부적절한 내용", LocalDateTime.of(2026, 7, 20, 10, 0));

        String message = AdminBotMessageFormatter.formatDetail(response);

        assertThat(message).contains("반려 사유: 부적절한 내용");
    }

    @Test
    @DisplayName("승인/반려 메시지는 testId와 상태를 포함한다")
    void formatApproveAndReject() {
        AdminTestStatusResponse response = new AdminTestStatusResponse(1L, TestStatus.IN_PROGRESS);

        assertThat(AdminBotMessageFormatter.formatApprove(response)).contains("1").contains("IN_PROGRESS");
        assertThat(AdminBotMessageFormatter.formatReject(response)).contains("1").contains("IN_PROGRESS");
    }
}
