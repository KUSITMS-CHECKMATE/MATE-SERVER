package server.MATE.domain.question.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.MATE.domain.question.dto.request.FiveSecondCreateRequest;
import server.MATE.domain.question.dto.response.FiveSecondCreateResponse;
import server.MATE.domain.question.service.FiveSecondService;
import server.MATE.global.common.response.ApiResponse;

@Tag(name = "[QUESTION] 문항 API", description = "문항 등록 관련 API")
@RestController
@RequestMapping("/api/v1/tests/{testId}/questions")
@RequiredArgsConstructor
public class FiveSecondController {

    private final FiveSecondService fiveSecondService;

    @Operation(summary = "5초 테스트 문항 등록", description = """
            5초 테스트 문항을 등록합니다.

            **[이미지 (imageKey)]**
            - 이미지는 필수입니다. 이미지 업로드 URL 발급 API로 먼저 업로드한 뒤 받은 imageKey를 전달합니다.

            **[객관식 전환 (isObjective)]**
            - false: 이미지만 노출하고 참여자가 자유롭게 답변합니다. options, isDuplicate, minSelect, maxSelect는 무시됩니다.
            - true: 이미지 하단에 선택지가 노출됩니다. 선택지는 최소 2개 이상 필요합니다.

            **[중복 선택 (isDuplicate)] - isObjective=true일 때만 유효**
            - true: 여러 선택지 선택 가능. minSelect, maxSelect로 선택 개수 범위 설정 가능 (선택값)
            - false: 단일 선택만 가능.

            **[에러 코드]**
            | 코드 | HTTP | 설명 |
            |------|------|------|
            | COMMON_002 | 400 | 요청 값 검증 실패 (필수 필드 누락, 글자수 초과 등) |
            | QUESTION_001 | 400 | 최소 선택 개수는 1 이상이어야 합니다 |
            | QUESTION_002 | 400 | 최대 선택 개수는 최소 선택 개수 이상이어야 합니다 |
            | QUESTION_003 | 400 | 최소/최대 선택 개수는 선택지 개수를 초과할 수 없습니다 |
            | QUESTION_004 | 400 | 객관식 전환 시 선택지는 최소 2개 이상이어야 합니다 |
            | TEST_004 | 404 | 테스트를 찾을 수 없음 |
            | TEST_005 | 403 | 테스트 제작자만 접근할 수 있음 |
            """)
    @PostMapping("/fivesecond")
    public ResponseEntity<ApiResponse<FiveSecondCreateResponse>> createFiveSecond(
            @Parameter(description = "문항을 등록할 테스트 ID")
            @PathVariable Long testId,
            @RequestBody @Valid FiveSecondCreateRequest request,
            @Parameter(description = "테스트 제작자 ID (인증 구현 전 임시 헤더)")
            @RequestHeader("X-User-Id") Long makerId
    ) {
        FiveSecondCreateResponse response = fiveSecondService.createFiveSecond(testId, makerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("5초 테스트 문항이 등록되었습니다.", response));
    }
}
