package server.MATE.domain.test.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.MATE.domain.test.dto.request.TestCreateRequest;
import server.MATE.domain.test.dto.response.TestCreateResponse;
import server.MATE.domain.test.service.TestService;
import server.MATE.global.common.response.ApiResponse;

@Tag(name = "[TEST] 테스트 API", description = "테스트 등록 관련 API")
@RestController
@RequestMapping("/api/v1/tests")
@RequiredArgsConstructor
public class TestController {

    private final TestService testService;

    @Operation(summary = "테스트 등록", description = """
            새로운 테스트를 등록합니다.

            **[categories]**
            DAILY, FINANCE, HEALTH, SHOPPING, FOOD, GAME, CONTENT, COMMUNITY,
            AI, EDUCATION, TRAVEL, SOCIAL, CONVENIENCE, INFORMATION, BUSINESS, TRANSPORT, PUBLIC_ADMIN
            (1~3개 선택 필수)

            **[이미지 처리]**
            - imageKeys는 이미지 업로드 URL 발급 API로 먼저 업로드한 뒤 받은 imageKey 목록입니다.
            - 트랜잭션 실패(롤백) 시 업로드된 이미지는 Azure Blob Storage에서 자동 삭제됩니다.

            **[에러 코드]**
            | 코드 | HTTP | 설명 |
            |------|------|------|
            | COMMON_002 | 400 | 요청 값 검증 실패 (필수 필드 누락, 글자수 초과 등) |
            | TEST_001 | 400 | 카테고리는 1~3개 선택 필수 |
            | TEST_002 | 400 | 이미지는 최대 10개까지 업로드 가능 |
            """)
    @PostMapping
    public ResponseEntity<ApiResponse<TestCreateResponse>> createTest(
            @RequestBody @Valid TestCreateRequest request,
            // TODO: 인증 구현 후 @AuthenticationPrincipal 등으로 대체
            @Parameter(description = "테스트 제작자 ID (인증 구현 전 임시 헤더)")
            @RequestHeader("X-User-Id") Long makerId
    ) {
        TestCreateResponse response = testService.createTest(request, makerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("테스트가 등록되었습니다.", response));
    }
}
