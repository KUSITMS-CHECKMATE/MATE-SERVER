package server.MATE.question.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.MATE.global.common.response.ApiResponse;
import server.MATE.question.dto.cardCategoryCreateRequest;
import server.MATE.question.dto.treeTestCreateRequest;
import server.MATE.question.service.questionService;

@Tag(name = "Question", description = "질문 API")
@RestController
@RequestMapping("/api/v1/question")
@RequiredArgsConstructor
public class questionController {

    private final questionService questionService;

    @PostMapping("/cardsorting")
    public ResponseEntity<ApiResponse<Void>> createCategories(
            @Valid @RequestBody cardCategoryCreateRequest request
    ) {
        questionService.cardSortingCreation(request);
        return ResponseEntity.ok(ApiResponse.ok("카드소팅 테스트 저장 완료", null));
    }

    @PostMapping("/treetest")
    public ResponseEntity<ApiResponse<Void>> createTreeTest(
            @Valid @RequestBody treeTestCreateRequest request
    ) {
        questionService.treeTestCreation(request);
        return ResponseEntity.ok(ApiResponse.ok("트리 테스트 저장 완료", null));
    }

}
