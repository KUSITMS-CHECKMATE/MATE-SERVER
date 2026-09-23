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
import server.MATE.domain.payment.entity.PublishStatus;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByPayStatus(PayStatus payStatus);

    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findFirstByDraftIdAndPayStatusOrderByCreatedAtDesc(Long draftId, PayStatus payStatus);

    Optional<Payment> findByTestId(Long testId);

    List<Payment> findByMakerIdOrderByCreatedAtDesc(Long makerId);

    List<Payment> findTop100ByPayStatusAndTestIdIsNullAndPublishStatus(PayStatus payStatus, PublishStatus publishStatus);

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

    // publishStatus 컬럼 추가 시 ddl-auto가 DB 기본값(PUBLISH_PENDING)만 채우고 기존 row는 보정하지 않으므로,
    // testId/retryCount로 이미 확정된 상태를 기동 시점에 한 번 백필한다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Payment p
            set p.publishStatus = :published
            where p.testId is not null and p.publishStatus <> :published
            """)
    int backfillPublishedStatus(@Param("published") PublishStatus published);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Payment p
            set p.publishStatus = :failed
            where p.testId is null and p.retryCount >= :retryLimit and p.publishStatus <> :failed
            """)
    int backfillFailedStatus(@Param("failed") PublishStatus failed, @Param("retryLimit") int retryLimit);
}
