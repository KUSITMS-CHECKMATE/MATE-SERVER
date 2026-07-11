package server.MATE.domain.test.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TestCloseSchedulerTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Mock
    private ThreadPoolTaskScheduler taskScheduler;

    @Mock
    private TestCloseProcessor testCloseProcessor;

    private TestCloseScheduler testCloseScheduler;

    @BeforeEach
    void setUp() {
        testCloseScheduler = new TestCloseScheduler(taskScheduler, testCloseProcessor);
    }

    @Test
    @DisplayName("closedAt이 있으면 해당 시각에 자동 종료를 예약한다")
    void schedulesCloseAtClosedAt() {
        LocalDateTime closedAt = LocalDateTime.of(2099, 12, 31, 23, 59, 59);

        testCloseScheduler.schedule(10L, closedAt);

        ArgumentCaptor<Instant> triggerCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(taskScheduler).schedule(any(Runnable.class), triggerCaptor.capture());
        assertThat(triggerCaptor.getValue()).isEqualTo(closedAt.atZone(KST).toInstant());
    }

    @Test
    @DisplayName("closedAt이 없으면 자동 종료 예약을 건너뛴다")
    void skipsScheduleWhenClosedAtIsNull() {
        testCloseScheduler.schedule(10L, null);

        verify(taskScheduler, never()).schedule(any(Runnable.class), any(Instant.class));
    }
}
