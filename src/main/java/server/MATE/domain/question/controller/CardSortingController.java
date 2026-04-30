package server.MATE.domain.question.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.MATE.domain.question.dto.CardSortingCreateRequest;
import server.MATE.domain.question.service.CardSortingService;
import server.MATE.global.common.response.ApiResponse;

@Tag(name = "CardSorting", description = "카드소팅 API")
@RestController
@RequestMapping("/api/v1/tests/{testId}/questions/{questionId}/cardsorting")
@RequiredArgsConstructor
public class CardSortingController {

    private final CardSortingService cardSortingService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createCardSorting(
            @PathVariable Long testId,
            @PathVariable Long questionId,
            @Valid @RequestBody CardSortingCreateRequest request
    ) {
        cardSortingService.createCardSorting(questionId, request);
        return ResponseEntity.ok(ApiResponse.ok("카드소팅 테스트 저장 완료", null));
    }
}
