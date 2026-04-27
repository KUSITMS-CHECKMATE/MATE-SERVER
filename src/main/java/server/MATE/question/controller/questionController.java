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
        return ResponseEntity.ok(ApiResponse.ok("카테고리 저장 완료", null));
    }

}
