package server.MATE.domain.payment.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import server.MATE.domain.payment.entity.Payment;

import java.util.List;
import java.util.Optional;

import server.MATE.domain.payment.entity.PayStatus;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByPayStatus(PayStatus payStatus);

    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findByTestId(Long testId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.testId = :testId")
    Optional<Payment> findByTestIdForUpdate(@Param("testId") Long testId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete
            from Payment p
            where p.testId = :testId
            """)
    int deleteAllByTestId(@Param("testId") Long testId);
}
