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
import server.MATE.domain.test.dto.request.TestCreateRequest;
import server.MATE.domain.test.dto.response.TestCreateResponse;
import server.MATE.domain.test.service.TestService;
import server.MATE.global.common.response.ApiResponse;
import server.MATE.global.security.principal.AuthenticatedUser;

@Tag(name = "[TEST] 테스트 API", description = "테스트 등록 관련 API")
@RestController
@RequestMapping("/api/v1/tests")
@SecurityRequirement(name = "JWT")
@RequiredArgsConstructor
public class TestController {

    private final TestService testService;

    @Operation(summary = "테스트 등록", description = "새로운 테스트를 등록합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<TestCreateResponse>> createTest(
            @RequestBody @Valid TestCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        TestCreateResponse response = testService.createTest(request, authenticatedUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("테스트가 등록되었습니다.", response));
    }
}
