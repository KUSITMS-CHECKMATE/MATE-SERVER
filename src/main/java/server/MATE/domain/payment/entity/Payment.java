package server.MATE.domain.payment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.MATE.global.common.entity.BaseEntity;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "payment",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "order_no"),
                @UniqueConstraint(columnNames = "test_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long draftId;

    @Column(name = "test_id")
    private Long testId;

    @Column(nullable = false)
    private Long makerId;

    @Column(name = "order_no", nullable = false, length = 50)
    private String orderNo;

    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PayStatus payStatus;

    @Column(nullable = false)
    private Integer goalPpl;

    @Column(nullable = false)
    private Integer reward;

    @Column(nullable = false)
    private Integer amount;

    private Integer paidAmount;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PayMethod payMethod;

    @Column(nullable = false)
    private Boolean isTestPayment;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(length = 200)
    private String refundReason;

    public void linkTest(Long testId) {
        this.testId = testId;
    }

    public void requestRefund(String reason) {
        this.payStatus = PayStatus.REFUND_PENDING;
        this.refundReason = reason;
    }

    public void completeRefund() {
        this.payStatus = PayStatus.REFUNDED;
    }

    @Builder
    public Payment(Long draftId,
                   Long testId,
                   Long makerId,
                   String orderNo,
                   String transactionId,
                   PayStatus payStatus,
                   Integer goalPpl,
                   Integer reward,
                   Integer amount,
                   Integer paidAmount,
                   PayMethod payMethod,
                   Boolean isTestPayment,
                   LocalDateTime approvedAt) {
        this.draftId = draftId;
        this.testId = testId;
        this.makerId = makerId;
        this.orderNo = orderNo;
        this.transactionId = transactionId;
        this.payStatus = payStatus;
        this.goalPpl = goalPpl;
        this.reward = reward;
        this.amount = amount;
        this.paidAmount = paidAmount;
        this.payMethod = payMethod;
        this.isTestPayment = isTestPayment == null ? Boolean.FALSE : isTestPayment;
        this.approvedAt = approvedAt;
    }
}
