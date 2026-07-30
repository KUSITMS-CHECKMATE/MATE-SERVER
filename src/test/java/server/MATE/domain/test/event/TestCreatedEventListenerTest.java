package server.MATE.domain.test.event;

import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import server.MATE.global.discord.channel.TestAlertChannel;

@ExtendWith(MockitoExtension.class)
class TestCreatedEventListenerTest {

    @Mock
    private TestAlertChannel testAlertChannel;

    @InjectMocks
    private TestCreatedEventListener listener;

    @Test
    @DisplayName("커밋 후 TestCreatedEvent를 받으면 TestAlertChannel에 알림을 위임한다")
    void handleTestCreated_delegatesToTestAlertChannel() {
        TestCreatedEvent event = new TestCreatedEvent(1L, "제목", 200, LocalDateTime.now());

        listener.handleTestCreated(event);

        verify(testAlertChannel).notifyCreated(event);
    }
}
