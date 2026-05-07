package server.MATE.domain.question.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.MATE.domain.question.dto.request.QuestionCreateRequest;
import server.MATE.domain.question.dto.response.QuestionCreateResponse;
import server.MATE.domain.question.service.QuestionService;
import server.MATE.global.common.response.ApiResponse;
import server.MATE.global.security.principal.AuthenticatedUser;

@Tag(name = "[QUESTION] 문항 API", description = "문항 등록 관련 API")
@RestController
@RequestMapping("/api/v1/tests/{testId}/questions")
@SecurityRequirement(name = "JWT")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @Operation(summary = "질문 문항 일괄 등록", description = """
            여러 유형의 질문 문항을 한 번에 등록합니다. MKTT_03 (질문 목록) 에 해당하는 api 입니다.
            - `questions` 배열 순서대로 sequence가 부여됩니다.
            - 같은 유형 중복 요청, 없는 유형 생략이 가능합니다.
            
            **[type]**
            - OBJECTIVE, SUBJECTIVE, FIVE_SECOND, SCALE, AB_TEST, CARD_SORTING, TREE_TEST
            - 각 문항은 `type` 필드로 유형을 구분합니다.
            """)
    @PostMapping
    public ResponseEntity<ApiResponse<QuestionCreateResponse>> createQuestions(
            @PathVariable Long testId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "질문 문항 일괄 등록 예시",
                                    value = """
                                            {
                                              "questions": [
                                                {
                                                  "type": "OBJECTIVE",
                                                  "title": "가장 자주 사용하는 기능은 무엇인가요?",
                                                  "description": "해당 서비스를 사용할 때 가장 자주 쓰는 기능을 골라주세요.",
                                                  "isDuplicate": false,
                                                  "isOther": true,
                                                  "options": [
                                                    { "content": "검색", "imageKey": null },
                                                    { "content": "결제", "imageKey": "objective-option-image-key" }
                                                  ]
                                                },
                                                {
                                                  "type": "SUBJECTIVE",
                                                  "title": "개선이 필요한 점은 무엇인가요?",
                                                  "description": "자유롭게 작성해주세요.",
                                                  "imageKey": null
                                                },
                                                {
                                                  "type": "FIVE_SECOND",
                                                  "title": "첫 화면에서 눈에 띄는 요소는 무엇인가요?",
                                                  "description": "이미지를 5초간 본 뒤 답변해주세요.",
                                                  "imageKey": "five-second-image-key",
                                                  "isObjective": true,
                                                  "isDuplicate": false,
                                                  "minSelect": null,
                                                  "maxSelect": null,
                                                  "options": [
                                                    { "content": "검색창" },
                                                    { "content": "배너" }
                                                  ]
                                                },
                                                {
                                                  "type": "SCALE",
                                                  "title": "전반적인 만족도를 평가해주세요.",
                                                  "description": "5점 척도로 응답해주세요.",
                                                  "imageKey": null,
                                                  "minLabel": "매우 불만족",
                                                  "maxLabel": "매우 만족",
                                                  "range": 5
                                                },
                                                {
                                                  "type": "AB_TEST",
                                                  "title": "어느 시안이 더 마음에 드시나요?",
                                                  "description": "두 시안을 비교하고 더 선호하는 쪽을 선택해주세요.",
                                                  "aImageKey": "ab-test-image-a",
                                                  "bImageKey": "ab-test-image-b"
                                                },
                                                {
                                                  "type": "CARD_SORTING",
                                                  "title": "기능 카드를 그룹으로 묶어주세요.",
                                                  "description": "비슷하다고 생각하는 항목끼리 분류해주세요.",
                                                  "categories": ["검색", "결제", "주문내역", "프로필"]
                                                },
                                                {
                                                  "type": "TREE_TEST",
                                                  "title": "설정 메뉴에서 알림 설정을 어디서 찾으시겠어요?",
                                                  "description": "예상되는 경로를 따라 선택해주세요.",
                                                  "features": [
                                                    {
                                                      "label": "마이페이지",
                                                      "children": [
                                                        {
                                                          "label": "설정",
                                                          "children": [
                                                            { "label": "알림 설정", "children": [] }
                                                          ]
                                                        }
                                                      ]
                                                    }
                                                  ]
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            )
            @RequestBody @Valid QuestionCreateRequest request
    ) {
        QuestionCreateResponse response = questionService.createQuestions(testId, authenticatedUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("문항이 등록되었습니다.", response));
    }
}
