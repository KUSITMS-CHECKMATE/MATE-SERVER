package server.MATE.domain.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.MATE.domain.payment.entity.Payment;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByIdAndMakerId(Long id, Long makerId);

    Optional<Payment> findByOrderNo(String orderNo);

    Optional<Payment> findByDraftId(Long draftId);
}
