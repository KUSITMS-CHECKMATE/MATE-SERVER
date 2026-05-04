package server.MATE.domain.question.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.MATE.domain.question.dto.request.AbTestCreateRequest;
import server.MATE.domain.question.dto.response.AbTestCreateResponse;
import server.MATE.domain.question.service.AbTestService;
import server.MATE.global.common.response.ApiResponse;

@Tag(name = "[QUESTION] 문항 API", description = "문항 등록 관련 API")
@RestController
@RequestMapping("/api/v1/tests/{testId}/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final AbTestService abTestService;

    @Operation(summary = "A/B 테스트 문항 등록", description = "A/B 테스트 문항을 등록합니다.")
    @PostMapping("/abtest")
    public ResponseEntity<ApiResponse<AbTestCreateResponse>> createAbTest(
            @PathVariable Long testId,
            @RequestBody @Valid AbTestCreateRequest request,
            // TODO: 인증 구현 후 @AuthenticationPrincipal 등으로 대체
            @RequestHeader("X-User-Id") Long makerId
    ) {
        AbTestCreateResponse response = abTestService.createAbTest(testId, makerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("A/B 테스트 질문이 등록되었습니다.", response));
    }
}
