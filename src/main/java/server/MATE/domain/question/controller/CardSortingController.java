package server.MATE.domain.question.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import server.MATE.domain.question.dto.request.CardSortingCreateRequest;
import server.MATE.domain.question.dto.response.CardSortingCreateResponse;
import server.MATE.domain.question.service.CardSortingService;
import server.MATE.global.common.response.ApiResponse;

@Tag(name = "[QUESTION] 문항 API", description = "문항 등록 관련 API")
@RestController
@RequestMapping("/api/v1/tests/{testId}/questions/cardsorting")
@RequiredArgsConstructor
public class CardSortingController {

    private final CardSortingService cardSortingService;

    @Operation(summary = "카드소팅 문항 등록", description = "카드소팅 문항을 등록합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<CardSortingCreateResponse>> createCardSorting(
            @PathVariable Long testId,
            @RequestBody @Valid CardSortingCreateRequest request,
            // TODO: 나중에 대체
            @RequestHeader("X-User-Id") Long makerId
    ) {
        CardSortingCreateResponse response = cardSortingService.createCardSorting(testId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("카드소팅 문항이 등록되었습니다.", response));
    }
}
