package server.MATE.domain.test.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import server.MATE.domain.test.dto.request.TestResponseRequest;
import server.MATE.domain.test.dto.response.TestResponseCreateResponse;
import server.MATE.domain.test.service.TestResponseService;
import server.MATE.global.common.response.ApiResponse;
import server.MATE.global.security.principal.AuthenticatedUser;

@Tag(name = "[TEST] 테스트 API", description = "테스트 등록 관련 API")
@RestController
@RequestMapping("/api/v1/tests/{testId}/responses")
@SecurityRequirement(name = "JWT")
@RequiredArgsConstructor
public class TestResponseController {

    private final TestResponseService testResponseService;

    @Operation(summary = "테스트 통합 응답 등록", description = """
            참여자가 테스트의 모든 문항에 대한 응답을 한 번에 제출합니다.

            - 테스트가 진행 중(IN_PROGRESS)이고 승인(ACCEPTED) 상태여야 합니다.
            - 목표 인원(goalPpl)이 초과된 경우 참여 불가합니다.
            - 이미 참여한 테스트에 중복 제출 불가합니다.
            - `answers` 배열의 각 항목은 기존 응답 등록 API와 동일한 형식입니다.
            - 모든 응답이 정상 저장된 후 pplCount가 증가합니다.

            **[에러 코드]**
            | 코드 | HTTP | 설명 |
            |------|------|------|
            | TEST_004 | 404 | 테스트를 찾을 수 없습니다 |
            | PARTICIPATION_002 | 400 | 참여할 수 없는 테스트입니다 |
            | PARTICIPATION_003 | 400 | 이미 참여한 테스트입니다 |
            | PARTICIPATION_004 | 400 | 테스트 참여 인원이 마감되었습니다 |
            | ANSWER_001 | 400 | 질문 타입과 응답 타입 불일치 |
            | ANSWER_002 | 400 | 질문이 해당 테스트에 속하지 않습니다 |
            | ANSWER_003 | 400 | 동일 질문에 대한 응답이 중복되었습니다 |
            | ANSWER_004 | 400 | 유효하지 않은 입력입니다 |
            | ANSWER_005 | 400 | 응답이 입력되지 않았습니다 |
            """)
    @PostMapping
    public ResponseEntity<ApiResponse<TestResponseCreateResponse>> submitResponse(
            @PathVariable Long testId,
            @RequestBody @Valid TestResponseRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        TestResponseCreateResponse response = testResponseService.submitResponse(testId, authenticatedUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("응답이 등록되었습니다.", response));
    }
}
