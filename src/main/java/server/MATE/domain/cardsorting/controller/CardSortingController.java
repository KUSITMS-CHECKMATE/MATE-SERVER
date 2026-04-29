package server.MATE.domain.cardsorting.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.MATE.domain.cardsorting.dto.CardSortingCreateRequest;
import server.MATE.domain.cardsorting.service.CardSortingService;
import server.MATE.global.common.response.ApiResponse;

@Tag(name = "CardSorting", description = "카드소팅 API")
@RestController
@RequestMapping("/api/v1/question/cardsorting")
@RequiredArgsConstructor
public class CardSortingController {

    private final CardSortingService cardSortingService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createCardSorting(
            @Valid @RequestBody CardSortingCreateRequest request
    ) {
        cardSortingService.createCardSorting(request);
        return ResponseEntity.ok(ApiResponse.ok("카드소팅 테스트 저장 완료", null));
    }
}
