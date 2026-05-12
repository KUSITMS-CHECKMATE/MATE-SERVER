package server.MATE.domain.answer.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import server.MATE.domain.answer.dto.request.ObjectiveAnswerCreateRequest;
import server.MATE.domain.answer.dto.request.SubjectiveAnswerCreateRequest;
import server.MATE.domain.answer.dto.response.AnswerCreateResponse;
import server.MATE.domain.answer.service.AnswerService;
import server.MATE.global.common.response.ApiResponse;
import server.MATE.global.security.principal.AuthenticatedUser;

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
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestBody @Valid SubjectiveAnswerCreateRequest request
    ) {
        AnswerCreateResponse response = answerService.createSubjectiveAnswer(participationId, authenticatedUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("응답이 등록되었습니다.", response));
    }

    @Operation(summary = "객관식 응답 등록", description = """
            객관식 질문에 대한 응답을 등록합니다.
            - questionId는 OBJECTIVE 타입이어야 합니다.
            - 질문은 해당 참여 세션의 테스트에 속해야 합니다.
            - selectedOptionIds는 해당 질문의 선택지 ID만 허용됩니다.
            - 단일 선택(isDuplicate=false)이면 selectedOptionIds는 정확히 1개여야 합니다.
            - 복수 선택(isDuplicate=true)이면 minSelect 이상 maxSelect 이하로 선택해야 합니다.
            - isOther=true인 질문에서만 otherText를 입력할 수 있습니다.

            **[에러 코드]**
            | 코드 | HTTP | 설명 |
            |------|------|------|
            | ANSWER_001 | 400 | questionId가 OBJECTIVE 타입이 아님 |
            | ANSWER_002 | 400 | 질문이 해당 테스트에 속하지 않음 |
            | ANSWER_003 | 400 | 이미 응답한 질문 |
            | ANSWER_004 | 400 | selectedOptionIds에 유효하지 않은 선택지 포함 |
            | ANSWER_005 | 400 | 선택 개수가 허용 범위를 벗어남 |
            """)
    @PostMapping("/objective")
    public ResponseEntity<ApiResponse<AnswerCreateResponse>> createObjectiveAnswer(
            @PathVariable Long participationId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestBody @Valid ObjectiveAnswerCreateRequest request
    ) {
        AnswerCreateResponse response = answerService.createObjectiveAnswer(participationId, authenticatedUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("응답이 등록되었습니다.", response));
    }
}
