package server.MATE.domain.promotion.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.MATE.domain.promotion.dto.response.PromotionRetryResponse;
import server.MATE.domain.promotion.service.PromotionRetryService;
import server.MATE.global.common.response.ApiResponse;

@Tag(name = "[ADMIN] 프로모션 관리 API", description = "관리자 프로모션 리워드 관련 API")
@RestController
@RequestMapping("/api/v1/admin/promotions")
@RequiredArgsConstructor
public class AdminPromotionController {

    private final PromotionRetryService promotionRetryService;

    @Operation(
            summary = "🔒 실패 리워드 재지급",
            description = """
                    FAILED 상태인 프로모션 리워드를 새 키 발급부터 다시 지급합니다. 현재 설정된 프로모션 코드로 실행됩니다.
                    토스가 지급을 실행하지 않은 것이 확실한 실패(TOSS_011, TOSS_012, TOSS_ACCOUNT_NOT_LINKED, PROMOTION_CODE_MISSING)만 허용합니다.
                    토스 API가 다시 실패하면 해당 토스 에러를 반환하고 리워드는 FAILED로 남습니다.
                    그 외에는 재지급 후 리워드 상태를 반환합니다. (SUCCEEDED: 지급 완료, PENDING: 스케줄러가 후속 확인,
                    FAILED: 토스 미연결 등으로 다시 실패 - lastErrorCode 확인)
                    """
    )
    @PostMapping("/rewards/{rewardId}/retry")
    public ResponseEntity<ApiResponse<PromotionRetryResponse>> retryReward(
            @PathVariable Long rewardId
    ) {
        PromotionRetryResponse response = promotionRetryService.retry(rewardId);
        return ResponseEntity.ok(ApiResponse.ok("리워드 재지급을 처리했습니다.", response));
    }
}
