package server.MATE.domain.test.event;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import server.MATE.domain.users.entity.TossAccount;
import server.MATE.domain.users.repository.TossAccountRepository;
import server.MATE.toss.config.TossMessengerProperties;
import server.MATE.toss.gateway.TossMessengerGateway;

@Slf4j
@Component
@RequiredArgsConstructor
public class TestApprovedEventListener {

    private final TossAccountRepository tossAccountRepository;
    private final TossMessengerGateway tossMessengerGateway;
    private final TossMessengerProperties tossMessengerProperties;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTestApproved(TestApprovedEvent event) {
        String templateSetCode = tossMessengerProperties.templates().testApproved();
        if (templateSetCode == null || templateSetCode.isBlank()) {
            log.warn("신규 테스트 안내 발송 코드가 설정되지 않아 발송을 건너뜁니다. testId={}", event.testId());
            return;
        }

        Long makerTossUserKey = tossAccountRepository.findByUserId(event.makerId())
                .map(TossAccount::getTossUserKey)
                .orElse(null);

        // 테스트를 등록한 메이커 본인에게는 신규 테스트 안내를 보내지 않는다.
        List<Long> tossUserKeys = tossAccountRepository.findTossUserKeysByIsLinkedTrue().stream()
                .filter(tossUserKey -> !tossUserKey.equals(makerTossUserKey))
                .toList();

        if (tossUserKeys.isEmpty()) {
            return;
        }

        tossMessengerGateway.sendBulk(
                templateSetCode,
                tossUserKeys,
                Map.of("title", event.title())
        );
    }
}
