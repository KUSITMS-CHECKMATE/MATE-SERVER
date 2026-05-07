package server.MATE.domain.question.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import server.MATE.domain.question.dto.request.TreeTestCreateRequest;
import server.MATE.domain.question.dto.response.TreeTestCreateResponse;
import server.MATE.domain.question.service.TreeTestService;
import server.MATE.global.common.response.ApiResponse;
import server.MATE.global.security.principal.AuthenticatedUser;

@Tag(name = "[QUESTION] 문항 API", description = "문항 등록 관련 API")
@RestController
@RequestMapping("/api/v1/tests/{testId}/questions/treetest")
@SecurityRequirement(name = "JWT")
@RequiredArgsConstructor
public class TreeTestController {

    private final TreeTestService treeTestService;

    @Operation(summary = "트리테스트 저장", description = "질문에 대한 트리테스트 노드 구조를 저장합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<TreeTestCreateResponse>> createTreeTest(
            @PathVariable Long testId,
            @Valid @RequestBody TreeTestCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        TreeTestCreateResponse response = treeTestService.createTreeTest(testId, authenticatedUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("트리테스트가 저장되었습니다.", response));
    }
}
