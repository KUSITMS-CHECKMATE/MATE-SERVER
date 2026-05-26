package server.MATE.toss.gateway;

import server.MATE.toss.dto.request.TossPaymentCreateRequest;
import server.MATE.toss.dto.request.TossPaymentExecuteRequest;
import server.MATE.toss.dto.request.TossPaymentRefundRequest;
import server.MATE.toss.dto.request.TossPaymentStatusRequest;
import server.MATE.toss.dto.response.TossPaymentCreateResponse;
import server.MATE.toss.dto.response.TossPaymentExecuteResponse;
import server.MATE.toss.dto.response.TossPaymentRefundResponse;
import server.MATE.toss.dto.response.TossPaymentStatusResponse;

public interface TossPaymentGateway {

    TossPaymentCreateResponse createPayment(TossPaymentCreateRequest request);

    TossPaymentExecuteResponse executePayment(TossPaymentExecuteRequest request);

    TossPaymentStatusResponse getPaymentStatus(TossPaymentStatusRequest request);

    TossPaymentRefundResponse refundPayment(TossPaymentRefundRequest request);
}
