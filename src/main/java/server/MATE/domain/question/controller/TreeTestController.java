package server.MATE.domain.question.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.MATE.domain.question.dto.TreeTestCreateRequest;
import server.MATE.domain.question.service.TreeTestService;
import server.MATE.global.common.response.ApiResponse;

@Tag(name = "TreeTest", description = "트리테스트 API")
@RestController
@RequestMapping("/api/v1/tests/{testId}/questions/{questionId}/treetest")
@RequiredArgsConstructor
public class TreeTestController {

    private final TreeTestService treeTestService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createTreeTest(
            @PathVariable Long testId,
            @PathVariable Long questionId,
            @Valid @RequestBody TreeTestCreateRequest request
    ) {
        treeTestService.createTreeTest(questionId, request);
        return ResponseEntity.ok(ApiResponse.ok("트리 테스트 저장 완료", null));
    }
}
