package server.MATE.domain.test.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import server.MATE.domain.test.dto.response.AdminTestListItemResponse;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.discord.channel.TestAlertChannel;

@ExtendWith(MockitoExtension.class)
class WaitingTestSummaryServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 24, 10, 0, 0);
    private static final LocalDateTime THRESHOLD = NOW.minusHours(4);

    @Mock
    private TestRepository testRepository;
    @Mock
    private TestAlertChannel testAlertChannel;

    private WaitingTestSummaryService service() {
        return new WaitingTestSummaryService(testRepository, testAlertChannel,
                Clock.fixed(NOW.atZone(KST).toInstant(), KST));
    }

    @Test
    @DisplayName("4시간 넘은 WAITING이 있으면 상위 항목과 전체 건수, 기준 시간을 넘긴다")
    @SuppressWarnings("unchecked")
    void notifiesWithItemsAndCount() {
        server.MATE.domain.test.entity.Test test = server.MATE.domain.test.entity.Test.builder()
                .makerId(1L).title("오래된 테스트").reward(300)
                .closedAt(LocalDateTime.of(2099, 12, 31, 23, 59, 59)).build();
        ReflectionTestUtils.setField(test, "id", 42L);
        ReflectionTestUtils.setField(test, "createdAt", NOW.minusHours(26));
        given(testRepository.findTop5ByTestStatusAndDeletedAtIsNullAndCreatedAtLessThanEqualOrderByCreatedAtAsc(
                TestStatus.WAITING, THRESHOLD)).willReturn(List.of(test));
        given(testRepository.countByTestStatusAndDeletedAtIsNullAndCreatedAtLessThanEqual(
                TestStatus.WAITING, THRESHOLD)).willReturn(7L);

        service().notifyLongWaitingTests();

        ArgumentCaptor<List<AdminTestListItemResponse>> captor = ArgumentCaptor.forClass(List.class);
        verify(testAlertChannel).notifyWaitingSummary(captor.capture(), eq(7L), eq(4));
        assertThat(captor.getValue()).extracting(AdminTestListItemResponse::testId).containsExactly(42L);
    }

    @Test
    @DisplayName("대상이 없으면 보내지 않는다")
    void skipsWhenEmpty() {
        given(testRepository.findTop5ByTestStatusAndDeletedAtIsNullAndCreatedAtLessThanEqualOrderByCreatedAtAsc(
                TestStatus.WAITING, THRESHOLD)).willReturn(List.of());

        service().notifyLongWaitingTests();

        verify(testAlertChannel, never()).notifyWaitingSummary(anyList(), anyLong(), anyInt());
    }
}
