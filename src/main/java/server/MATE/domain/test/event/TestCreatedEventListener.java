package server.MATE.domain.test.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import server.MATE.global.discord.channel.TestAlertChannel;

@Component
@RequiredArgsConstructor
public class TestCreatedEventListener {

    private final TestAlertChannel testAlertChannel;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTestCreated(TestCreatedEvent event) {
        testAlertChannel.notifyCreated(event);
    }
}
