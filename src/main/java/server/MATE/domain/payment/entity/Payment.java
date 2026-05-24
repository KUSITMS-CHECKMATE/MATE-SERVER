package server.MATE.domain.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.common.entity.BaseEntity;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "payment",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "order_no")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long draftId;

    private Long testId;

    @Column(nullable = false)
    private Long makerId;

    @Column(name = "order_no", nullable = false, length = 50)
    private String orderNo;

    private String payToken;

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

    @Column(length = 10)
    private String accountBankCode;

    @Column(length = 10)
    private String cardCompanyCode;

    @Column(nullable = false)
    private Boolean isTestPayment;

    private LocalDateTime approvalTime;

    public void markCreated(String payToken) {
        this.payToken = payToken;
        this.payStatus = PayStatus.PAY_CREATED;
    }

    public void prepareForRetry(String orderNo,
                                Integer goalPpl,
                                Integer reward,
                                Integer amount,
                                Boolean isTestPayment) {
        this.orderNo = orderNo;
        this.goalPpl = goalPpl;
        this.reward = reward;
        this.amount = amount;
        this.isTestPayment = isTestPayment;
        this.payToken = null;
        this.transactionId = null;
        this.paidAmount = null;
        this.payMethod = null;
        this.accountBankCode = null;
        this.cardCompanyCode = null;
        this.approvalTime = null;
        this.payStatus = PayStatus.PAY_STANDBY;
    }

    public void markFailed() {
        this.payStatus = PayStatus.PAY_FAILED;
    }

    public void markSucceeded(String transactionId,
                              Integer paidAmount,
                              PayMethod payMethod,
                              String accountBankCode,
                              String cardCompanyCode,
                              LocalDateTime approvalTime) {
        this.transactionId = transactionId;
        this.paidAmount = paidAmount;
        this.payMethod = payMethod;
        this.accountBankCode = accountBankCode;
        this.cardCompanyCode = cardCompanyCode;
        this.approvalTime = approvalTime;
        this.payStatus = PayStatus.PAY_SUCCEEDED;
    }

    public void markRefundPending() {
        this.payStatus = PayStatus.REFUND_PENDING;
    }

    public void markRefunded() {
        this.payStatus = PayStatus.REFUNDED;
    }

    public void markRefundFailed() {
        this.payStatus = PayStatus.REFUND_FAILED;
    }

    public void linkTest(Long testId) {
        this.testId = testId;
    }

    public void validateReadyToExecute() {
        if (this.payStatus != PayStatus.PAY_CREATED) {
            throw new BaseException(BaseErrorCode.PAYMENT_003);
        }
    }

    public void validateRefundable() {
        if (this.payStatus != PayStatus.PAY_SUCCEEDED) {
            throw new BaseException(BaseErrorCode.PAYMENT_004);
        }
    }

    @Builder
    public Payment(Long draftId,
                   Long testId,
                   Long makerId,
                   String orderNo,
                   String payToken,
                   String transactionId,
                   PayStatus payStatus,
                   Integer goalPpl,
                   Integer reward,
                   Integer amount,
                   Integer paidAmount,
                   PayMethod payMethod,
                   String accountBankCode,
                   String cardCompanyCode,
                   Boolean isTestPayment,
                   LocalDateTime approvalTime) {
        this.draftId = draftId;
        this.testId = testId;
        this.makerId = makerId;
        this.orderNo = orderNo;
        this.payToken = payToken;
        this.transactionId = transactionId;
        this.payStatus = payStatus == null ? PayStatus.PAY_STANDBY : payStatus;
        this.goalPpl = goalPpl;
        this.reward = reward;
        this.amount = amount;
        this.paidAmount = paidAmount;
        this.payMethod = payMethod;
        this.accountBankCode = accountBankCode;
        this.cardCompanyCode = cardCompanyCode;
        this.isTestPayment = isTestPayment == null ? Boolean.TRUE : isTestPayment;
        this.approvalTime = approvalTime;
    }
}
