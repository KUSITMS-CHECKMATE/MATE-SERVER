package server.MATE.domain.question.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.MATE.domain.question.dto.request.SubjectiveQuestionCreateRequest;
import server.MATE.domain.question.dto.response.SubjectiveQuestionCreateResponse;
import server.MATE.domain.question.service.SubjectiveQuestionService;
import server.MATE.global.common.response.ApiResponse;

@Tag(name = "[QUESTION] 문항 API", description = "문항 등록 관련 API")
@RestController
@RequestMapping("/api/v1/tests/{testId}/questions")
@RequiredArgsConstructor
public class SubjectiveQuestionController {

    private final SubjectiveQuestionService subjectiveQuestionService;

    @Operation(summary = "주관식 문항 등록", description = "주관식 문항을 등록합니다.")
    @PostMapping("/subjective")
    public ResponseEntity<ApiResponse<SubjectiveQuestionCreateResponse>> createSubjectiveQuestion(
            @PathVariable Long testId,
            @RequestBody @Valid SubjectiveQuestionCreateRequest request,
            // TODO: 인증 구현 후 @AuthenticationPrincipal 등으로 대체
            @RequestHeader("X-User-Id") Long makerId
    ) {
        SubjectiveQuestionCreateResponse response =
                subjectiveQuestionService.createSubjectiveQuestion(testId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("주관식 질문이 등록되었습니다.", response));
    }
}
