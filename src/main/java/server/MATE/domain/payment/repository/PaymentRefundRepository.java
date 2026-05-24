package server.MATE.domain.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.MATE.domain.payment.entity.PaymentRefund;

import java.util.List;
import java.util.Optional;

public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, Long> {

    List<PaymentRefund> findAllByPaymentIdOrderByApprovalTimeDesc(Long paymentId);

    Optional<PaymentRefund> findByRefundNo(String refundNo);
}
