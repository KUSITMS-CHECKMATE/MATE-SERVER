package server.MATE.domain.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.payment.entity.PayMethod;
import server.MATE.domain.payment.entity.Payment;
import server.MATE.domain.payment.repository.PaymentRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentExecuteStateService {

    private final PaymentRepository paymentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment markSucceeded(Long paymentId,
                                 Long makerId,
                                 String transactionId,
                                 Integer paidAmount,
                                 PayMethod payMethod,
                                 String accountBankCode,
                                 String cardCompanyCode,
                                 LocalDateTime approvalTime) {
        Payment payment = paymentRepository.findByIdAndMakerId(paymentId, makerId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.PAYMENT_001));

        payment.markSucceeded(
                transactionId,
                paidAmount,
                payMethod,
                accountBankCode,
                cardCompanyCode,
                approvalTime
        );
        return payment;
    }
}
