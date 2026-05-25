package server.MATE.domain.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.MATE.domain.payment.dto.response.PaymentCreateResponse;
import server.MATE.domain.payment.dto.response.PaymentExecuteResponse;
import server.MATE.domain.payment.dto.response.PaymentRefundResponse;
import server.MATE.domain.payment.dto.response.PaymentStatusResponse;
import server.MATE.domain.payment.dto.request.PaymentCreateRequest;
import server.MATE.domain.payment.dto.request.PaymentRefundRequest;
import server.MATE.domain.payment.service.MockPaymentService;
import server.MATE.global.common.response.ApiResponse;
import server.MATE.global.security.principal.AuthenticatedUser;

@Tag(name = "[MOCK PAYMENT] 결제 API", description = "Mock 기반 결제 관련 API")
@RestController
@RequestMapping("/api/v1/mock/payments")
@SecurityRequirement(name = "JWT")
@RequiredArgsConstructor
public class MockPaymentController {

    private final MockPaymentService mockPaymentService;

    @Operation(summary = "✔️ 결제 등록",
            description = """
                          테스트 초안을 기준으로 mock 결제를 등록하고 payToken을 반환합니다.
                          """
    )
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentCreateResponse>> createPayment(
            @RequestBody @Valid PaymentCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        PaymentCreateResponse response = mockPaymentService.createPayment(
                request.draftId(),
                authenticatedUser.getId(),
                request.isTestPayment() == null || request.isTestPayment()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("결제를 등록했습니다.", response));
    }

    @Operation(summary = "⚠️ 결제 실행",
            description = """
                          mock 결제를 실행하고 승인 결과를 반환합니다.
                          - 버그 사항: 부가세 포함하여 최종 결제 금액 산정
                          """
    )
    @PostMapping("/{paymentId}/execute")
    public ResponseEntity<ApiResponse<PaymentExecuteResponse>> executePayment(
            @PathVariable Long paymentId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        PaymentExecuteResponse response = mockPaymentService.executePayment(paymentId, authenticatedUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("결제를 실행했습니다.", response));
    }

    @Operation(summary = "결제 상태 조회", description = "mock 결제 상태를 조회합니다.")
    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> getPaymentStatus(
            @PathVariable Long paymentId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        PaymentStatusResponse response = mockPaymentService.getPaymentStatus(paymentId, authenticatedUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("결제 상태를 조회했습니다.", response));
    }

    @Operation(summary = "결제 환불", description = "mock 결제를 환불합니다.")
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<ApiResponse<PaymentRefundResponse>> refundPayment(
            @PathVariable Long paymentId,
            @RequestBody @Valid PaymentRefundRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        PaymentRefundResponse response = mockPaymentService.refundPayment(paymentId, authenticatedUser.getId(), request.reason());
        return ResponseEntity.ok(ApiResponse.ok("결제를 환불했습니다.", response));
    }
}
