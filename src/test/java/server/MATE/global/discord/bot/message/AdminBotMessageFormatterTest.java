package server.MATE.global.discord.bot.message;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.entities.MessageEmbed;
import server.MATE.domain.test.dto.response.AdminTestDetailResponse;
import server.MATE.domain.test.dto.response.AdminTestListItemResponse;
import server.MATE.domain.test.dto.response.AdminTestListResponse;
import server.MATE.domain.test.entity.TestStatus;

class AdminBotMessageFormatterTest {

    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 7, 29, 20, 46);

    @Test
    @DisplayName("statusLabel: 상태별 한글 라벨")
    void statusLabel() {
        assertThat(AdminBotMessageFormatter.statusLabel(TestStatus.WAITING)).isEqualTo("검토 대기");
        assertThat(AdminBotMessageFormatter.statusLabel(TestStatus.IN_PROGRESS)).isEqualTo("진행 중");
        assertThat(AdminBotMessageFormatter.statusLabel(TestStatus.REJECTED)).isEqualTo("반려됨");
        assertThat(AdminBotMessageFormatter.statusLabel(TestStatus.COMPLETED)).isEqualTo("종료");
    }

    @Test
    @DisplayName("listEmbed: 제목은 '{상태} 목록 조회', 항목은 이름/카테고리 백틱/리워드를 포함한다")
    void listEmbed() {
        AdminTestListItemResponse item = new AdminTestListItemResponse(
                14L, "봇승인테스트", 200, List.of("DAILY", "INFORMATION"), TestStatus.WAITING, CREATED);
        AdminTestListResponse response = new AdminTestListResponse(1, 5, 1, List.of(item));

        MessageEmbed embed = AdminBotMessageFormatter.listEmbed(TestStatus.WAITING, response);

        assertThat(embed.getTitle()).isEqualTo("검토 대기 목록 조회");
        assertThat(embed.getDescription())
                .contains("**#14 봇승인테스트**")
                .contains("`DAILY`, `INFORMATION`")
                .contains("💰 `200`");
    }

    @Test
    @DisplayName("listEmbed: 결과가 없으면 안내 문구")
    void listEmbed_empty() {
        AdminTestListResponse response = new AdminTestListResponse(1, 5, 0, List.of());
        MessageEmbed embed = AdminBotMessageFormatter.listEmbed(TestStatus.WAITING, response);
        assertThat(embed.getDescription()).contains("조회된 테스트가 없습니다.");
    }

    @Test
    @DisplayName("totalPages: 총 13건 · size 5 → 3페이지")
    void totalPages() {
        AdminTestListResponse response = new AdminTestListResponse(1, 5, 13, List.of());
        assertThat(AdminBotMessageFormatter.totalPages(response)).isEqualTo(3);
    }

    @Test
    @DisplayName("detailEmbed: 제목/이모지 라인/이미지 개수를 포함한다")
    void detailEmbed() {
        AdminTestDetailResponse detail = new AdminTestDetailResponse(
                14L, "봇승인테스트", "설명", 200,
                List.of("DAILY", "INFORMATION"),
                List.of("https://img/1.jpg", "https://img/2.jpg", "https://img/3.jpg",
                        "https://img/4.jpg", "https://img/5.jpg"),
                TestStatus.WAITING, null, CREATED);

        MessageEmbed embed = AdminBotMessageFormatter.detailEmbed(detail);

        assertThat(embed.getTitle()).isEqualTo("검토 대기 · #14 봇승인테스트");
        assertThat(embed.getDescription())
                .contains("💰 리워드 | `200P`")
                .contains("🏷️ 카테고리 | `DAILY`, `INFORMATION`")
                .contains("🖼️ 이미지 | `총 5장`");
        assertThat(embed.getThumbnail().getUrl()).isEqualTo("https://img/1.jpg");
    }

    @Test
    @DisplayName("resultEmbed: 반려는 사유와 숨김 안내를 포함한다")
    void resultEmbed_rejected() {
        AdminTestDetailResponse detail = new AdminTestDetailResponse(
                15L, "봇반려테스트", "설명", 200, List.of("DAILY"), List.of(),
                TestStatus.REJECTED, "부적절한 내용", CREATED);

        MessageEmbed embed = AdminBotMessageFormatter.resultEmbed(detail);

        assertThat(embed.getTitle()).isEqualTo("반려됨 · #15 봇반려테스트");
        assertThat(embed.getDescription())
                .contains("🚫 사유 | `부적절한 내용`")
                .contains("사용자 서비스에서 숨김 처리됩니다.");
    }

    @Test
    @DisplayName("actionButtons: 상태별 버튼 구성")
    void actionButtons() {
        assertThat(rowSize(AdminBotMessageFormatter.actionButtons(1L, TestStatus.WAITING))).isEqualTo(2);
        assertThat(rowSize(AdminBotMessageFormatter.actionButtons(1L, TestStatus.IN_PROGRESS))).isEqualTo(1);
        assertThat(rowSize(AdminBotMessageFormatter.actionButtons(1L, TestStatus.REJECTED))).isEqualTo(1);
        assertThat(AdminBotMessageFormatter.actionButtons(1L, TestStatus.COMPLETED)).isEmpty();
    }

    @Test
    @DisplayName("processLog: 하트/이름/상태전이/사유를 담는다")
    void processLog() {
        String log = AdminBotMessageFormatter.processLog(
                "💜", "박소윤", TestStatus.WAITING, TestStatus.REJECTED, "부적절한 내용");
        assertThat(log)
                .contains("💜 **박소윤** 님이 반려")
                .contains("`WAITING → REJECTED`")
                .contains("> 사유: 부적절한 내용");
    }

    @Test
    @DisplayName("processLog: 승인은 사유 줄이 없다")
    void processLog_approve() {
        String log = AdminBotMessageFormatter.processLog(
                "💚", "이선호", TestStatus.WAITING, TestStatus.IN_PROGRESS, null);
        assertThat(log)
                .contains("💚 **이선호** 님이 승인")
                .contains("`WAITING → IN_PROGRESS`")
                .doesNotContain("사유:");
    }

    private int rowSize(List<ActionRow> rows) {
        return rows.isEmpty() ? 0 : rows.get(0).getComponents().size();
    }
}
