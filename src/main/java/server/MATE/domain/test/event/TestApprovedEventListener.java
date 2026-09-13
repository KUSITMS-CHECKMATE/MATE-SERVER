package server.MATE.domain.test.event;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import server.MATE.domain.users.repository.TossAccountRepository;
import server.MATE.toss.gateway.TossMessengerGateway;

@Component
@RequiredArgsConstructor
public class TestApprovedEventListener {

    private static final String TEST_APPROVED_TEMPLATE_CODE = "mate-test-approved";

    private final TossAccountRepository tossAccountRepository;
    private final TossMessengerGateway tossMessengerGateway;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTestApproved(TestApprovedEvent event) {
        List<Long> tossUserKeys = tossAccountRepository.findTossUserKeysByIsLinkedTrue();

        if (tossUserKeys.isEmpty()) {
            return;
        }

        tossMessengerGateway.sendBulk(TEST_APPROVED_TEMPLATE_CODE, tossUserKeys, Map.of("title", event.title()));
    }
}
