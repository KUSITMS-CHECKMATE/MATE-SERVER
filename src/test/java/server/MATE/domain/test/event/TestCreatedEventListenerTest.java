package server.MATE.domain.test.event;

import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import server.MATE.global.discord.outbox.service.DiscordOutboxService;

@ExtendWith(MockitoExtension.class)
class TestCreatedEventListenerTest {

    @Mock
    private DiscordOutboxService discordOutboxService;

    @InjectMocks
    private TestCreatedEventListener listener;

    private final TestCreatedEvent event = new TestCreatedEvent(1L, "제목", 200, LocalDateTime.now());

    @Test
    @DisplayName("커밋 전: 발행 트랜잭션 안에서 outbox 행을 적재한다")
    void enqueueAlert() {
        listener.enqueueAlert(event);

        verify(discordOutboxService).enqueueTestCreated(1L);
    }

    @Test
    @DisplayName("커밋 후: 적재된 행을 바로 한 번 처리한다")
    void sendAlert() {
        listener.sendAlert(event);

        verify(discordOutboxService).processTestCreated(1L);
    }
}
