package server.MATE.domain.payment.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import server.MATE.domain.payment.entity.Payment;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByIdAndMakerId(Long id, Long makerId);

    Optional<Payment> findByOrderNo(String orderNo);

    Optional<Payment> findByDraftId(Long draftId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from Payment p
            where p.id = :id
            """)
    Optional<Payment> findByIdForUpdate(@Param("id") Long id);
}
