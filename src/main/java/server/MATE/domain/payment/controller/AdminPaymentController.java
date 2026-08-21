package server.MATE.domain.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.MATE.domain.payment.service.RefundService;
import server.MATE.global.common.response.ApiResponse;

@Tag(name = "[ADMIN] 결제 관리 API", description = "관리자 결제 관련 API")
@RestController
@RequestMapping("/api/v1/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final RefundService refundService;

    @Operation(
            summary = "🔒 환불 완료 처리",
            description = """
                    Toss 파트너 콘솔에서 환불 처리 완료 후 호출합니다. REFUND_PENDING 상태인 결제를 REFUNDED로 전환합니다.
                    """
    )
    @PostMapping("/{paymentId}/complete-refund")
    public ResponseEntity<ApiResponse<Void>> completeRefund(
            @PathVariable Long paymentId
    ) {
        refundService.completeRefund(paymentId);
        return ResponseEntity.ok(ApiResponse.ok("환불 완료 처리되었습니다.", null));
    }
}
