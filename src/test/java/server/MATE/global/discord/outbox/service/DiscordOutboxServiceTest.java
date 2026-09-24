package server.MATE.global.discord.outbox.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.discord.channel.TestAlertChannel;
import server.MATE.global.discord.outbox.entity.DiscordOutbox;
import server.MATE.global.discord.outbox.entity.DiscordOutboxStatus;
import server.MATE.global.discord.outbox.entity.DiscordOutboxType;
import server.MATE.global.discord.outbox.repository.DiscordOutboxRepository;

@ExtendWith(MockitoExtension.class)
class DiscordOutboxServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 24, 10, 0, 0);
    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 9, 24, 9, 59, 0);

    @Mock
    private DiscordOutboxRepository outboxRepository;
    @Mock
    private TestRepository testRepository;
    @Mock
    private TestAlertChannel testAlertChannel;

    private DiscordOutboxService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(KST).toInstant(), KST);
        service = new DiscordOutboxService(outboxRepository, testRepository, testAlertChannel,
                mock(PlatformTransactionManager.class), clock, "prod");
    }

    private DiscordOutbox pendingOutbox(long outboxId, long testId) {
        DiscordOutbox outbox = DiscordOutbox.pending(DiscordOutboxType.TEST_CREATED, testId, CREATED);
        org.springframework.test.util.ReflectionTestUtils.setField(outbox, "id", outboxId);
        return outbox;
    }

    private server.MATE.domain.test.entity.Test test(long testId, TestStatus status) {
        server.MATE.domain.test.entity.Test test = server.MATE.domain.test.entity.Test.builder()
                .makerId(1L)
                .title("새 테스트")
                .reward(500)
                .testStatus(status)
                .closedAt(LocalDateTime.of(2099, 12, 31, 23, 59, 59))
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(test, "id", testId);
        org.springframework.test.util.ReflectionTestUtils.setField(test, "createdAt", CREATED);
        return test;
    }

    @Test
    @DisplayName("enqueueTestCreated: PENDING 행을 저장한다")
    void enqueue_savesPending() {
        service.enqueueTestCreated(10L);

        ArgumentCaptor<DiscordOutbox> captor = ArgumentCaptor.forClass(DiscordOutbox.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getTargetId()).isEqualTo(10L);
        assertThat(captor.getValue().getType()).isEqualTo(DiscordOutboxType.TEST_CREATED);
        assertThat(captor.getValue().getNextAttemptAt()).isEqualTo(NOW.plusMinutes(1));
    }

    @Test
    @DisplayName("enqueueTestCreated: local 환경이면 저장하지 않는다")
    void enqueue_skipsInLocal() {
        DiscordOutboxService localService = new DiscordOutboxService(outboxRepository, testRepository,
                testAlertChannel, mock(PlatformTransactionManager.class), Clock.systemDefaultZone(), "local");

        localService.enqueueTestCreated(10L);

        verifyNoInteractions(outboxRepository);
    }

    @Test
    @DisplayName("process: WAITING 테스트면 현재 값으로 보내고 SENT")
    void process_sendsAndMarksSent() {
        DiscordOutbox outbox = pendingOutbox(1L, 10L);
        given(outboxRepository.findById(1L)).willReturn(Optional.of(outbox));
        given(testRepository.findById(10L)).willReturn(Optional.of(test(10L, TestStatus.WAITING)));

        service.process(1L);

        verify(testAlertChannel).sendCreated(10L, "새 테스트", 500, CREATED);
        assertThat(outbox.getStatus()).isEqualTo(DiscordOutboxStatus.SENT);
        assertThat(outbox.getSentAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("process: 전송 실패면 시도 횟수를 올리고 1분 뒤로 미룬다(예외는 던지지 않음)")
    void process_recordsFailure() {
        DiscordOutbox outbox = pendingOutbox(1L, 10L);
        given(outboxRepository.findById(1L)).willReturn(Optional.of(outbox));
        given(testRepository.findById(10L)).willReturn(Optional.of(test(10L, TestStatus.WAITING)));
        willThrow(new IllegalStateException("discord down"))
                .given(testAlertChannel).sendCreated(anyLong(), any(), anyInt(), any());

        service.process(1L);

        assertThat(outbox.getStatus()).isEqualTo(DiscordOutboxStatus.PENDING);
        assertThat(outbox.getAttemptCount()).isEqualTo(1);
        assertThat(outbox.getNextAttemptAt()).isEqualTo(NOW.plusMinutes(1));
        assertThat(outbox.getLastError()).contains("IllegalStateException").contains("discord down");
    }

    @Test
    @DisplayName("process: 5회째 실패하면 FAILED")
    void process_failsAfterMaxAttempts() {
        DiscordOutbox outbox = pendingOutbox(1L, 10L);
        for (int i = 0; i < DiscordOutbox.MAX_ATTEMPTS - 1; i++) {
            outbox.recordFailure("earlier", NOW);
        }
        given(outboxRepository.findById(1L)).willReturn(Optional.of(outbox));
        given(testRepository.findById(10L)).willReturn(Optional.of(test(10L, TestStatus.WAITING)));
        willThrow(new IllegalStateException("discord down"))
                .given(testAlertChannel).sendCreated(anyLong(), any(), anyInt(), any());

        service.process(1L);

        assertThat(outbox.getStatus()).isEqualTo(DiscordOutboxStatus.FAILED);
        assertThat(outbox.getAttemptCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("process: 테스트가 WAITING이 아니면 보내지 않고 SKIPPED")
    void process_skipsWhenNotWaiting() {
        DiscordOutbox outbox = pendingOutbox(1L, 10L);
        given(outboxRepository.findById(1L)).willReturn(Optional.of(outbox));
        given(testRepository.findById(10L)).willReturn(Optional.of(test(10L, TestStatus.IN_PROGRESS)));

        service.process(1L);

        verifyNoInteractions(testAlertChannel);
        assertThat(outbox.getStatus()).isEqualTo(DiscordOutboxStatus.SKIPPED);
    }

    @Test
    @DisplayName("process: 테스트가 없으면 SKIPPED")
    void process_skipsWhenTestMissing() {
        DiscordOutbox outbox = pendingOutbox(1L, 10L);
        given(outboxRepository.findById(1L)).willReturn(Optional.of(outbox));
        given(testRepository.findById(10L)).willReturn(Optional.empty());

        service.process(1L);

        verifyNoInteractions(testAlertChannel);
        assertThat(outbox.getStatus()).isEqualTo(DiscordOutboxStatus.SKIPPED);
    }

    @Test
    @DisplayName("process: 이미 PENDING이 아닌 행은 아무것도 하지 않는다")
    void process_ignoresNonPending() {
        DiscordOutbox outbox = pendingOutbox(1L, 10L);
        outbox.markSent(NOW);
        given(outboxRepository.findById(1L)).willReturn(Optional.of(outbox));

        service.process(1L);

        verify(testRepository, never()).findById(anyLong());
        verifyNoInteractions(testAlertChannel);
    }

    @Test
    @DisplayName("process: 전송 도중 다른 워커가 먼저 SENT로 바꾸면 실패 기록으로 덮어쓰지 않는다")
    void process_doesNotOverwriteConcurrentlySentRow() {
        DiscordOutbox outbox = pendingOutbox(1L, 10L);
        given(outboxRepository.findById(1L)).willReturn(Optional.of(outbox));
        given(testRepository.findById(10L)).willReturn(Optional.of(test(10L, TestStatus.WAITING)));
        willAnswer(invocation -> {
            outbox.markSent(NOW);
            throw new IllegalStateException("discord down");
        }).given(testAlertChannel).sendCreated(anyLong(), any(), anyInt(), any());

        service.process(1L);

        assertThat(outbox.getStatus()).isEqualTo(DiscordOutboxStatus.SENT);
        assertThat(outbox.getAttemptCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("processTestCreated: 행이 없으면(local 등) 아무것도 하지 않는다")
    void processTestCreated_noRow() {
        given(outboxRepository.findByTypeAndTargetId(DiscordOutboxType.TEST_CREATED, 10L)).willReturn(Optional.empty());

        service.processTestCreated(10L);

        verifyNoInteractions(testRepository, testAlertChannel);
    }
}
