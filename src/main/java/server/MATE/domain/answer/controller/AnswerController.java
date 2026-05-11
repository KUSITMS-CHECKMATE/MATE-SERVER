package server.MATE.domain.answer.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.MATE.domain.answer.dto.request.SubjectiveAnswerCreateRequest;
import server.MATE.domain.answer.dto.response.AnswerCreateResponse;
import server.MATE.domain.answer.service.AnswerService;
import server.MATE.global.common.response.ApiResponse;

@Tag(name = "[ANSWER] 응답 API", description = "테스트 응답 관련 API")
@RestController
@RequestMapping("/api/v1/participations/{participationId}/answers")
@SecurityRequirement(name = "JWT")
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    @Operation(summary = "주관식 응답 등록", description = """
            주관식 질문에 대한 응답을 등록합니다.
            - questionId는 SUBJECTIVE 타입이어야 합니다.
            - 질문은 해당 참여 세션의 테스트에 속해야 합니다.
            """)
    @PostMapping("/subjective")
    public ResponseEntity<ApiResponse<AnswerCreateResponse>> createSubjectiveAnswer(
            @PathVariable Long participationId,
            @RequestBody @Valid SubjectiveAnswerCreateRequest request
    ) {
        AnswerCreateResponse response = answerService.createSubjectiveAnswer(participationId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("응답이 등록되었습니다.", response));
    }
}
