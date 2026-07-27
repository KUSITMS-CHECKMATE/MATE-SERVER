package server.MATE.domain.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.MATE.domain.payment.dto.response.PaymentHistoryResponse;
import server.MATE.domain.payment.service.PaymentHistoryService;
import server.MATE.global.common.response.ApiResponse;
import server.MATE.global.security.principal.AuthenticatedUser;

import java.util.List;

@Tag(name = "[PAYMENT] 결제 API", description = "결제 관련 API")
@RestController
@RequestMapping("/api/v1/payments")
@SecurityRequirement(name = "JWT")
@RequiredArgsConstructor
public class PaymentHistoryController {

    private final PaymentHistoryService paymentHistoryService;

    @Operation(
            summary = "결제 내역 조회",
            description = """
                    로그인한 메이커의 결제 내역을 최신순으로 조회합니다.<br>
                    응답: 결제일시, 테스트명, 결제금액, 결제상태, 주문번호
                    """
    )
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<PaymentHistoryResponse>>> getHistory(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        List<PaymentHistoryResponse> history = paymentHistoryService.getHistory(authenticatedUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("결제 내역을 조회했습니다.", history));
    }
}
