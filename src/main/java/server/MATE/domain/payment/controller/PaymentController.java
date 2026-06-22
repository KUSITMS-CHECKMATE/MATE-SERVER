package server.MATE.domain.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.MATE.domain.payment.dto.request.PaymentGrantRequest;
import server.MATE.domain.payment.service.PaymentGrantService;
import server.MATE.global.common.response.ApiResponse;
import server.MATE.global.security.principal.AuthenticatedUser;

@Tag(name = "[PAYMENT] 결제 API", description = "결제 관련 API")
@RestController
@RequestMapping("/api/v1/payments")
@SecurityRequirement(name = "JWT")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentGrantService paymentGrantService;

    @Operation(
            summary = "인앱결제 상품 지급 처리",
            description = """
                    Toss 인앱결제 SDK의 processProductGrant 콜백에서 호출하는 엔드포인트입니다.<br>
                    orderId로 Toss 결제 상태를 검증(PURCHASED 또는 PAYMENT_COMPLETED)한 뒤, 테스트를 게시하고 결제 내역을 저장합니다.<br>
                    지급 성공 시 true, 실패 시 false를 반환합니다.
                    """
    )
    @PostMapping("/grant")
    public ResponseEntity<ApiResponse<Boolean>> grant(
            @RequestBody @Valid PaymentGrantRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        boolean granted = paymentGrantService.grant(
                request.orderId(),
                request.draftId(),
                authenticatedUser.getId()
        );
        return ResponseEntity.ok(ApiResponse.ok("상품 지급 처리가 완료됐습니다.", granted));
    }
}
