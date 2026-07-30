package server.MATE.global.discord.bot.interaction;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import server.MATE.domain.test.entity.TestStatus;

class AdminBotCustomIdsTest {

    @Test
    @DisplayName("승인/반려 custom id를 만들고 되파싱한다")
    void approveRejectRoundTrip() {
        String approve = AdminBotCustomIds.approve(14L, TestStatus.WAITING);
        AdminBotCustomIds.Parsed p = AdminBotCustomIds.parse(approve);

        assertThat(AdminBotCustomIds.isAdmin(approve)).isTrue();
        assertThat(p.action()).isEqualTo(AdminBotCustomIds.APPROVE);
        assertThat(p.testId()).isEqualTo(14L);
        assertThat(p.fromStatus()).isEqualTo(TestStatus.WAITING);
    }

    @Test
    @DisplayName("목록 이동 custom id는 상태와 대상 페이지를 담는다")
    void listRoundTrip() {
        String list = AdminBotCustomIds.list(TestStatus.IN_PROGRESS, 3);
        AdminBotCustomIds.Parsed p = AdminBotCustomIds.parse(list);

        assertThat(p.action()).isEqualTo(AdminBotCustomIds.LIST);
        assertThat(p.listStatus()).isEqualTo(TestStatus.IN_PROGRESS);
        assertThat(p.targetPage()).isEqualTo(3);
    }

    @Test
    @DisplayName("admin 접두사가 아니면 isAdmin은 false")
    void isAdmin_false() {
        assertThat(AdminBotCustomIds.isAdmin("other:thing")).isFalse();
        assertThat(AdminBotCustomIds.isAdmin(null)).isFalse();
    }
}
