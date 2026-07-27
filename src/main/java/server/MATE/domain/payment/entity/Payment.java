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
                @UniqueConstraint(columnNames = "order_id"),
                @UniqueConstraint(columnNames = "test_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * TestPublishService가 발행 후 draft를 삭제하므로, 발행 이후에는 존재하지 않는
     * row를 가리키는 댕글링 참조가 된다. testId가 채워지면 그쪽이 진짜 참조이고,
     * draftId는 감사/디버깅용 스냅샷이다.
     */
    @Column(nullable = false)
    private Long draftId;

    @Column(name = "test_id")
    private Long testId;

    @Column(nullable = false)
    private Long makerId;

    @Column(name = "order_id", nullable = false, length = 50)
    private String orderId;

    @Column(name = "order_no", nullable = false, length = 20, unique = true)
    private String orderNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PayStatus payStatus;

    @Column(nullable = false)
    private Integer goalPpl;

    @Column(nullable = false)
    private Integer reward;

    @Column(nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PayMethod payMethod;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(length = 200)
    private String refundReason;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int retryCount;

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

    public void incrementRetryCount() {
        this.retryCount++;
    }

    @Builder
    public Payment(Long draftId,
                   Long testId,
                   Long makerId,
                   String orderId,
                   String orderNo,
                   PayStatus payStatus,
                   Integer goalPpl,
                   Integer reward,
                   Integer amount,
                   PayMethod payMethod,
                   LocalDateTime approvedAt) {
        this.draftId = draftId;
        this.testId = testId;
        this.makerId = makerId;
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.payStatus = payStatus;
        this.goalPpl = goalPpl;
        this.reward = reward;
        this.amount = amount;
        this.payMethod = payMethod;
        this.approvedAt = approvedAt;
    }
}
