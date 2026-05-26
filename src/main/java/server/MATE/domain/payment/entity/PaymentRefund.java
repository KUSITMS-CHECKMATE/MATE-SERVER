package server.MATE.domain.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.MATE.global.common.entity.BaseEntity;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "payment_refund",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "refund_no")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentRefund extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long paymentId;

    @Column(name = "refund_no", nullable = false)
    private String refundNo;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private Integer refundedAmount;

    @Column(nullable = false)
    private String transactionId;

    @Column(name = "approved_at", nullable = false)
    private LocalDateTime approvedAt;

    @Builder
    public PaymentRefund(Long paymentId,
                         String refundNo,
                         String reason,
                         Integer refundedAmount,
                         String transactionId,
                         LocalDateTime approvedAt) {
        this.paymentId = paymentId;
        this.refundNo = refundNo;
        this.reason = reason;
        this.refundedAmount = refundedAmount;
        this.transactionId = transactionId;
        this.approvedAt = approvedAt;
    }
}
