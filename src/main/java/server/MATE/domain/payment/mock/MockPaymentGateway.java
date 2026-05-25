package server.MATE.domain.payment.mock;

import org.springframework.stereotype.Component;
import server.MATE.domain.payment.entity.PayMethod;
import server.MATE.domain.payment.entity.PayStatus;
import server.MATE.toss.dto.request.TossPaymentCreateRequest;
import server.MATE.toss.dto.request.TossPaymentExecuteRequest;
import server.MATE.toss.dto.request.TossPaymentRefundRequest;
import server.MATE.toss.dto.request.TossPaymentStatusRequest;
import server.MATE.toss.dto.response.TossPaymentCreateResponse;
import server.MATE.toss.dto.response.TossPaymentExecuteResponse;
import server.MATE.toss.dto.response.TossPaymentRefundResponse;
import server.MATE.toss.dto.response.TossPaymentStatusResponse;
import server.MATE.toss.gateway.TossPaymentGateway;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class MockPaymentGateway implements TossPaymentGateway {

    private final Clock clock;

    public MockPaymentGateway(Clock clock) {
        this.clock = clock;
    }

    @Override
    public TossPaymentCreateResponse createPayment(TossPaymentCreateRequest request) {
        return new TossPaymentCreateResponse("mock-pay-token-" + request.orderNo());
    }

    @Override
    public TossPaymentExecuteResponse executePayment(TossPaymentExecuteRequest request) {
        return new TossPaymentExecuteResponse(
                request.orderNo(),
                0,
                LocalDateTime.now(clock),
                0,
                PayMethod.TOSS_MONEY,
                request.payToken(),
                UUID.randomUUID().toString(),
                "092",
                null
        );
    }

    @Override
    public TossPaymentStatusResponse getPaymentStatus(TossPaymentStatusRequest request) {
        return new TossPaymentStatusResponse(
                request.payToken(),
                request.orderNo(),
                PayStatus.PAY_SUCCEEDED,
                PayMethod.TOSS_MONEY,
                0,
                0,
                UUID.randomUUID().toString(),
                LocalDateTime.now(clock)
        );
    }

    @Override
    public TossPaymentRefundResponse refundPayment(TossPaymentRefundRequest request) {
        return new TossPaymentRefundResponse(
                "mock-refund-" + UUID.randomUUID(),
                LocalDateTime.now(clock),
                0,
                request.payToken(),
                UUID.randomUUID().toString()
        );
    }
}
