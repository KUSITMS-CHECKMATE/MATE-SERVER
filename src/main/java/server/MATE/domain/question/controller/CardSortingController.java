package server.MATE.domain.question.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import server.MATE.domain.question.dto.request.CardSortingCreateRequest;
import server.MATE.domain.question.dto.response.CardSortingCreateResponse;
import server.MATE.domain.question.service.CardSortingService;
import server.MATE.global.common.response.ApiResponse;
import server.MATE.global.security.principal.AuthenticatedUser;

@Tag(name = "[QUESTION] 문항 API", description = "문항 등록 관련 API")
@RestController
@RequestMapping("/api/v1/tests/{testId}/questions/cardsorting")
@SecurityRequirement(name = "JWT")
@RequiredArgsConstructor
public class CardSortingController {

    private final CardSortingService cardSortingService;

    @Operation(
            summary = "카드소팅 문항 등록",
            description = """
                    카드소팅 문항을 등록합니다.

                    **[요청 규칙]**
                    - cards: 최소 4개, 최대 12개
                    - categories: 최소 1개, 최대 3개 (`name` 필드 필수)
                    - categories는 JSON 배열로 저장됩니다.

                    **[권한]**
                    - JWT 인증이 필요합니다.
                    - 테스트 제작자만 등록할 수 있습니다.

                    **[에러 코드]**
                    | 코드 | HTTP | 설명 |
                    |------|------|------|
                    | COMMON_002 | 400 | 요청 값 검증 실패 |
                    | TEST_004 | 404 | 테스트를 찾을 수 없음 |
                    | TEST_005 | 403 | 테스트 제작자가 아님 |
                    """
    )
    @PostMapping
    public ResponseEntity<ApiResponse<CardSortingCreateResponse>> createCardSorting(
            @Parameter(description = "문항을 등록할 테스트 ID")
            @PathVariable Long testId,
            @RequestBody @Valid CardSortingCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        CardSortingCreateResponse response = cardSortingService.createCardSorting(
                testId,
                authenticatedUser.getId(),
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("카드소팅 문항이 등록되었습니다.", response));
    }
}
