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
import server.MATE.domain.question.dto.request.SubjectiveCreateRequest;
import server.MATE.domain.question.dto.response.SubjectiveCreateResponse;
import server.MATE.domain.question.service.SubjectiveService;
import server.MATE.global.common.response.ApiResponse;
import server.MATE.global.security.principal.AuthenticatedUser;

@Tag(name = "[QUESTION] 문항 API", description = "문항 등록 관련 API")
@RestController
@RequestMapping("/api/v1/tests/{testId}/questions")
@SecurityRequirement(name = "JWT")
@RequiredArgsConstructor
public class SubjectiveController {

    private final SubjectiveService subjectiveService;

    @Operation(summary = "주관식 문항 등록", description = """
            주관식 문항을 등록합니다.

            **[imageKey]**
            - 문항에 이미지를 첨부할 경우, 이미지 업로드 URL 발급 API로 먼저 업로드한 뒤 받은 imageKey를 전달합니다.
            - 이미지가 없으면 null로 보내거나 필드를 생략합니다.

            **[에러 코드]**
            | 코드 | HTTP | 설명 |
            |------|------|------|
            | COMMON_002 | 400 | 요청 값 검증 실패 (title 필수, 글자수 초과 등) |
            | TEST_004 | 404 | 테스트를 찾을 수 없음 |
            """)
    @PostMapping("/subjective")
    public ResponseEntity<ApiResponse<SubjectiveCreateResponse>> createSubjectiveQuestion(
            @Parameter(description = "문항을 등록할 테스트 ID")
            @PathVariable Long testId,
            @RequestBody @Valid SubjectiveCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        SubjectiveCreateResponse response =
                subjectiveService.createSubjective(testId, authenticatedUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("주관식 질문이 등록되었습니다.", response));
    }
}
