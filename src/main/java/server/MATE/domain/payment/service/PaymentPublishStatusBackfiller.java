package server.MATE.domain.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import server.MATE.domain.payment.entity.Payment;
import server.MATE.domain.payment.entity.PublishStatus;
import server.MATE.domain.payment.repository.PaymentRepository;

// publishStatus 컬럼은 ddl-auto가 기존 row에 DB 기본값(PUBLISH_PENDING)만 채우고 넘어가므로,
// 이미 testId/retryCount로 실제 상태가 확정된 row를 기동 시점마다 멱등하게 보정한다.
// 영구 상주용이 아니라 이번 컬럼 추가 배포에 대한 일회성 보정 도구다. 배포 로그에서
// "Payment publishStatus 백필 완료" 로그로 정상 실행을 확인한 뒤, 이 클래스와
// PaymentRepository.backfillPublishedStatus/backfillFailedStatus를 삭제할 것.
@Slf4j
@Component
@ConditionalOnProperty(prefix = "toss.api", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class PaymentPublishStatusBackfiller implements ApplicationRunner {

    private final PaymentRepository paymentRepository;

    @Override
    public void run(ApplicationArguments args) {
        // 배포 시점에 진행 중인 grant()/restore() 트랜잭션과 같은 row를 두고 락 경합이 날 수 있다.
        // 백필은 언제든 다음 기동 때 다시 시도해도 안전(멱등)하므로, 실패해도 앱 기동 자체를
        // 막지 않도록 여기서 흡수한다.
        try {
            int publishedUpdated = paymentRepository.backfillPublishedStatus(PublishStatus.PUBLISHED);
            int failedUpdated = paymentRepository.backfillFailedStatus(PublishStatus.FAILED, Payment.MAX_RESTORE_RETRY_COUNT);

            if (publishedUpdated > 0 || failedUpdated > 0) {
                log.info("Payment publishStatus 백필 완료. published={}, failed={}", publishedUpdated, failedUpdated);
            }
        } catch (Exception e) {
            log.warn("Payment publishStatus 백필 실패. 다음 기동 시 재시도된다.", e);
        }
    }
}
