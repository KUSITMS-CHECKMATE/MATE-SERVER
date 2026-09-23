package server.MATE.domain.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.payment.entity.Payment;
import server.MATE.domain.payment.entity.PublishStatus;
import server.MATE.domain.payment.repository.PaymentRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

// IapService는 트랜잭션 경계가 없어 restore()가 락 없이 읽은 Payment를 그대로 save()하면
// 동시 요청(grant()의 publish 커밋 등)을 merge로 덮어써버릴 수 있다. 재시도 횟수 체크·증가·FAILED
// 전환을 이 안에서 락을 잡고 독립 트랜잭션으로 원자적으로 처리해 그 레이스를 막는다.
@Component
@RequiredArgsConstructor
public class PaymentRestoreAttemptService {

    private final PaymentRepository paymentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PublishStatus registerRestoreAttempt(Long paymentId) {
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.PAYMENT_001));

        if (payment.getPublishStatus() == PublishStatus.PUBLISHED) {
            return PublishStatus.PUBLISHED;
        }

        if (payment.getRetryCount() >= Payment.MAX_RESTORE_RETRY_COUNT) {
            payment.markPublishFailed();
            return PublishStatus.FAILED;
        }

        payment.incrementRetryCount();
        return PublishStatus.PUBLISH_PENDING;
    }
}
