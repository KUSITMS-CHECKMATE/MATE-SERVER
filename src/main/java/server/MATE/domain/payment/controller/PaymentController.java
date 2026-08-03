package server.MATE.domain.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.MATE.domain.payment.dto.request.PaymentGrantRequest;
import server.MATE.domain.payment.dto.request.PaymentRestoreRequest;
import server.MATE.domain.payment.dto.response.PaymentOrderStatusResponse;
import server.MATE.domain.payment.service.IapService;
import server.MATE.global.common.response.ApiResponse;
import server.MATE.global.security.principal.AuthenticatedUser;

@Tag(name = "[PAYMENT] 결제 API", description = "결제 관련 API")
@RestController
@RequestMapping("/api/v1/payments")
@SecurityRequirement(name = "JWT")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "toss.api", name = "enabled", havingValue = "true")
public class PaymentController {

    private final IapService iapService;

    @Operation(
            summary = "인앱결제 상품 지급 처리",
            description = """
                    Toss 인앱결제 SDK의 processProductGrant 콜백에서 호출하는 엔드포인트입니다.<br>
                    orderId로 Toss 결제 상태를 검증(PURCHASED 또는 PAYMENT_COMPLETED)한 뒤, 테스트를 게시하고 결제 내역을 저장합니다.<br>
                    지급에 성공하면 200을 반환하고, 실패하면 원인별 에러 코드와 함께 4xx/5xx를 반환합니다.<br>
                    PAYMENT_013(409, 결제 진행 중)은 잠시 후 재시도하면 성공할 수 있는 케이스입니다.
                    """
    )
    @PostMapping("/grant")
    public ResponseEntity<ApiResponse<Void>> grant(
            @RequestBody @Valid PaymentGrantRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        iapService.grant(
                request.orderId(),
                request.draftId(),
                authenticatedUser.getId()
        );
        return ResponseEntity.ok(ApiResponse.ok("상품 지급 처리가 완료됐습니다."));
    }

    @Operation(
            summary = "미결 주문 복원 (상품 지급 재시도)",
            description = """
                    getPendingOrders로 조회된 미결 주문의 상품 지급을 재시도합니다.<br>
                    이전 grant 호출에서 Payment가 저장됐으면 orderId만으로 publish를 재시도합니다.<br>
                    Payment가 없으면 draftId를 함께 전달해야 합니다.<br>
                    복원에 성공하면 200을 반환하고, 실패하면 원인별 에러 코드와 함께 4xx/5xx를 반환합니다.
                    """
    )
    @PostMapping("/restore")
    public ResponseEntity<ApiResponse<Void>> restore(
            @RequestBody @Valid PaymentRestoreRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        iapService.restore(
                request.orderId(),
                request.draftId(),
                authenticatedUser.getId()
        );
        return ResponseEntity.ok(ApiResponse.ok("상품 지급 복원이 완료됐습니다."));
    }

    @Operation(
            summary = "인앱결제 주문 상태 조회",
            description = """
                    Toss IAP 주문 상태를 직접 조회합니다.<br>
                    네트워크 오류, 콜백 미수신 등 예외 상황에서 결제 상태를 확인할 때 사용합니다.<br>
                    응답: status, reason, statusDeterminedAt
                    """
    )
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<PaymentOrderStatusResponse>> getOrderStatus(
            @PathVariable String orderId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        PaymentOrderStatusResponse response = iapService.getOrderStatus(orderId, authenticatedUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("결제 상태를 조회했습니다.", response));
    }
}
