package server.MATE.domain.question.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.MATE.domain.question.dto.request.QuestionCreateRequest;
import server.MATE.domain.question.dto.response.QuestionCreateResponse;
import server.MATE.domain.question.dto.response.QuestionDetailResponse;
import server.MATE.domain.question.service.QuestionService;
import server.MATE.global.common.response.ApiResponse;
import server.MATE.global.security.principal.AuthenticatedUser;

@Tag(name = "[QUESTION] 질문 API", description = "테스트 질문 관련 API")
@RestController
@RequestMapping("/api/v1/tests/{testId}/questions")
@SecurityRequirement(name = "JWT")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @Operation(summary = "질문 전체 조회", description = """
            testId에 해당한 테스트의 모든 질문 문항을 상세조회합니다. TT01-01 화면에 해당하는 api 입니다.
            - 응답 루트에 `testId`와 `questions`를 함께 반환합니다.
            - `questions` 배열은 `sequence` 오름차순입니다.
            - 각 질문 유지은 공통 필드와 유형별 상세 필드를 모두 포함합니다.
            """)
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "객관식 조회",
                                            summary = "OBJECTIVE 조회 예시",
                                            value = """
                                                    {
                                                      "success": true,
                                                      "code": "200",
                                                      "message": "문항을 조회했습니다.",
                                                      "data": {
                                                        "testId": 10,
                                                        "questions": [
                                                          {
                                                            "questionId": 101,
                                                            "objectiveId": 101,
                                                            "type": "OBJECTIVE",
                                                            "sequence": 1,
                                                            "title": "가장 자주 사용하는 기능은 무엇인가요?",
                                                            "description": "해당 서비스를 사용할 때 가장 자주 쓰는 기능을 골라주세요.",
                                                            "isDuplicate": false,
                                                            "minSelect": null,
                                                            "maxSelect": null,
                                                            "isOther": true,
                                                            "options": [
                                                              { "objectiveOptionId": 1001, "content": "검색", "imageKey": null, "sequence": 1, "isOtherOption": false },
                                                              { "objectiveOptionId": 1002, "content": "결제", "imageKey": "objective-option-image-key", "sequence": 2, "isOtherOption": false },
                                                              { "objectiveOptionId": 1099, "content": "기타 (직접 입력)", "imageKey": null, "sequence": 3, "isOtherOption": true }
                                                            ]
                                                          }
                                                        ]
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "주관식 조회",
                                            summary = "SUBJECTIVE 조회 예시",
                                            value = """
                                                    {
                                                      "success": true,
                                                      "code": "200",
                                                      "message": "문항을 조회했습니다.",
                                                      "data": {
                                                        "testId": 10,
                                                        "questions": [
                                                          {
                                                            "questionId": 102,
                                                            "subjectiveId": 102,
                                                            "type": "SUBJECTIVE",
                                                            "sequence": 2,
                                                            "title": "개선이 필요한 점은 무엇인가요?",
                                                            "description": "자유롭게 작성해주세요.",
                                                            "imageKey": "subjective-image-key"
                                                          }
                                                        ]
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "5초 테스트 조회 (객관식)",
                                            summary = "FIVE_SECOND 객관식 조회 예시",
                                            value = """
                                                    {
                                                      "success": true,
                                                      "code": "200",
                                                      "message": "문항을 조회했습니다.",
                                                      "data": {
                                                        "testId": 10,
                                                        "questions": [
                                                          {
                                                            "questionId": 103,
                                                            "fiveSecondId": 103,
                                                            "type": "FIVE_SECOND",
                                                            "sequence": 3,
                                                            "title": "첫 화면에서 눈에 띄는 요소는 무엇인가요?",
                                                            "description": "이미지를 5초간 본 뒤 답변해주세요.",
                                                            "imageKey": "five-second-image-key",
                                                            "imageRatio": "9:16",
                                                            "isObjective": true,
                                                            "isDuplicate": true,
                                                            "minSelect": 1,
                                                            "maxSelect": 3,
                                                            "isOther": true,
                                                            "options": [
                                                              { "fiveSecondOptionId": 2001, "content": "검색창", "sequence": 1, "isOtherOption": false },
                                                              { "fiveSecondOptionId": 2002, "content": "메인 배너", "sequence": 2, "isOtherOption": false },
                                                              { "fiveSecondOptionId": 2099, "content": "기타 (직접 입력)", "sequence": 3, "isOtherOption": true }
                                                            ]
                                                          }
                                                        ]
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "5초 테스트 조회 (주관식)",
                                            summary = "FIVE_SECOND 주관식 조회 예시",
                                            value = """
                                                    {
                                                      "success": true,
                                                      "code": "200",
                                                      "message": "문항을 조회했습니다.",
                                                      "data": {
                                                        "testId": 10,
                                                        "questions": [
                                                          {
                                                            "questionId": 103,
                                                            "fiveSecondId": 103,
                                                            "type": "FIVE_SECOND",
                                                            "sequence": 3,
                                                            "title": "첫 화면에서 가장 먼저 떠오른 점은 무엇인가요?",
                                                            "description": "이미지를 5초간 본 뒤 자유롭게 작성해주세요.",
                                                            "imageKey": "five-second-image-key",
                                                            "imageRatio": "9:16",
                                                            "isObjective": false,
                                                            "isDuplicate": null,
                                                            "minSelect": null,
                                                            "maxSelect": null,
                                                            "isOther": null,
                                                            "options": []
                                                          }
                                                        ]
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "척도형 조회",
                                            summary = "SCALE 조회 예시",
                                            value = """
                                                    {
                                                      "success": true,
                                                      "code": "200",
                                                      "message": "문항을 조회했습니다.",
                                                      "data": {
                                                        "testId": 10,
                                                        "questions": [
                                                          {
                                                            "questionId": 104,
                                                            "scaleId": 104,
                                                            "type": "SCALE",
                                                            "sequence": 4,
                                                            "title": "전반적인 만족도를 평가해주세요.",
                                                            "description": "5점 척도로 응답해주세요.",
                                                            "imageKey": null,
                                                            "minLabel": "매우 불만족",
                                                            "maxLabel": "매우 만족",
                                                            "range": 5
                                                          }
                                                        ]
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "AB 테스트 조회",
                                            summary = "AB_TEST 조회 예시",
                                            value = """
                                                    {
                                                      "success": true,
                                                      "code": "200",
                                                      "message": "문항을 조회했습니다.",
                                                      "data": {
                                                        "testId": 10,
                                                        "questions": [
                                                          {
                                                            "questionId": 105,
                                                            "abTestId": 105,
                                                            "type": "AB_TEST",
                                                            "sequence": 5,
                                                            "title": "어느 시안이 더 마음에 드시나요?",
                                                            "description": "두 시안을 비교하고 더 선호하는 쪽을 선택해주세요.",
                                                            "aImageKey": "image-a.jpg",
                                                            "bImageKey": "image-b.jpg",
                                                            "imageRatio": "9:16"
                                                          }
                                                        ]
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "카드소팅 조회",
                                            summary = "CARD_SORTING 조회 예시",
                                            value = """
                                                    {
                                                      "success": true,
                                                      "code": "200",
                                                      "message": "문항을 조회했습니다.",
                                                      "data": {
                                                        "testId": 10,
                                                        "questions": [
                                                          {
                                                            "questionId": 106,
                                                            "cardSortingId": 106,
                                                            "type": "CARD_SORTING",
                                                            "sequence": 6,
                                                            "title": "기능 카드를 그룹으로 묶어주세요.",
                                                            "description": "비슷하다고 생각하는 항목끼리 분류해주세요.",
                                                            "cards": ["티셔츠", "꽃무늬가 들어간 티셔츠", "찢어진 청바지", "닥터마틴 워커"],
                                                            "categories": ["상의", "하의", "신발"]
                                                          }
                                                        ]
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "트리 테스트 조회",
                                            summary = "TREE_TEST 조회 예시",
                                            value = """
                                                    {
                                                      "success": true,
                                                      "code": "200",
                                                      "message": "문항을 조회했습니다.",
                                                      "data": {
                                                        "testId": 10,
                                                        "questions": [
                                                          {
                                                            "questionId": 107,
                                                            "type": "TREE_TEST",
                                                            "sequence": 7,
                                                            "title": "설정 메뉴에서 알림 설정을 어디서 찾으시겠어요?",
                                                            "description": "예상되는 경로를 따라 선택해주세요.",
                                                            "features": [
                                                              {
                                                                "treeTestId": 3001,
                                                                "label": "마이페이지",
                                                                "children": [
                                                                  {
                                                                    "treeTestId": 3002,
                                                                    "label": "설정",
                                                                    "children": [
                                                                      {
                                                                        "treeTestId": 3003,
                                                                        "label": "알림 설정",
                                                                        "children": []
                                                                      }
                                                                    ]
                                                                  }
                                                                ]
                                                              }
                                                            ]
                                                          }
                                                        ]
                                                      }
                                                    }
                                                    """
                                    )
                            }
                    ))
    })
    @GetMapping
    public ResponseEntity<ApiResponse<QuestionDetailResponse>> getQuestions(
            @PathVariable Long testId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        QuestionDetailResponse response = questionService.getQuestions(testId, authenticatedUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("문항을 조회했습니다.", response));
    }

    @Operation(summary = "질문 전체 등록", description = """
            여러 유형의 질문 문항을 한 번에 등록합니다. MKTT_03 (질문 목록) 화면에 해당하는 api 입니다.
            - `questions` 배열 순서대로 `sequence`가 부여됩니다.
            - 같은 유형 중복 요청, 없는 유형 생략이 가능합니다.
            - `FIVE_SECOND`, `OBJECTIVE`에서 `isOther`=true면 기타 (직접 입력) 선택지는 서버가 자동 생성합니다.
            
            질문 유형
            - 각 문항은 `type` 필드로 유형을 구분합니다.
            - OBJECTIVE, SUBJECTIVE, FIVE_SECOND, SCALE, AB_TEST, CARD_SORTING, TREE_TEST
            """)
    @PostMapping
    public ResponseEntity<ApiResponse<QuestionCreateResponse>> createQuestions(
            @PathVariable Long testId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "객관식",
                                            summary = "OBJECTIVE 등록 예시",
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
                                                        }
                                                      ]
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "주관식",
                                            summary = "SUBJECTIVE 등록 예시",
                                            value = """
                                                    {
                                                      "questions": [
                                                        {
                                                          "type": "SUBJECTIVE",
                                                          "title": "개선이 필요한 점은 무엇인가요?",
                                                          "description": "자유롭게 작성해주세요.",
                                                          "imageKey": null
                                                        }
                                                      ]
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "5초 테스트 (객관식)",
                                            summary = "FIVE_SECOND 객관식 등록 예시",
                                            value = """
                                                    {
                                                      "questions": [
                                                        {
                                                          "type": "FIVE_SECOND",
                                                          "title": "첫 화면에서 가장 먼저 보인 요소는 무엇인가요?",
                                                          "description": "이미지를 5초간 본 뒤 눈에 들어온 요소를 최대 3개까지 골라주세요.",
                                                          "imageKey": "tt01-home-hero-image",
                                                          "imageRatio": "9:16",
                                                          "isObjective": true,
                                                          "isDuplicate": true,
                                                          "minSelect": 1,
                                                          "maxSelect": 3,
                                                          "isOther": true,
                                                          "options": [
                                                            { "content": "상단 검색창" },
                                                            { "content": "메인 배너" },
                                                            { "content": "카테고리 메뉴" },
                                                            { "content": "하단 CTA 버튼" }
                                                          ]
                                                        }
                                                      ]
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "5초 테스트 (주관식)",
                                            summary = "FIVE_SECOND 주관식 등록 예시",
                                            value = """
                                                    {
                                                      "questions": [
                                                        {
                                                          "type": "FIVE_SECOND",
                                                          "title": "첫 화면에서 가장 먼저 떠오른 점은 무엇인가요?",
                                                          "description": "이미지를 5초간 본 뒤 자유롭게 작성해주세요.",
                                                          "imageKey": "five-second-image-key",
                                                          "imageRatio": "9:16",
                                                          "isObjective": false
                                                        }
                                                      ]
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "척도형",
                                            summary = "SCALE 등록 예시",
                                            value = """
                                                    {
                                                      "questions": [
                                                        {
                                                          "type": "SCALE",
                                                          "title": "전반적인 만족도를 평가해주세요.",
                                                          "description": "5점 척도로 응답해주세요.",
                                                          "imageKey": null,
                                                          "minLabel": "매우 불만족",
                                                          "maxLabel": "매우 만족",
                                                          "range": 5
                                                        }
                                                      ]
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "AB 테스트",
                                            summary = "AB_TEST 등록 예시",
                                            value = """
                                                    {
                                                      "questions": [
                                                        {
                                                          "type": "AB_TEST",
                                                          "title": "어느 시안이 더 마음에 드시나요?",
                                                          "description": "두 시안을 비교하고 더 선호하는 쪽을 선택해주세요.",
                                                          "aImageKey": "image-a.jpg",
                                                          "bImageKey": "image-b.jpg",
                                                          "imageRatio": "9:16"
                                                        }
                                                      ]
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "카드소팅",
                                            summary = "CARD_SORTING 등록 예시",
                                            value = """
                                                    {
                                                      "questions": [
                                                        {
                                                          "type": "CARD_SORTING",
                                                          "title": "기능 카드를 그룹으로 묶어주세요.",
                                                          "description": "비슷하다고 생각하는 항목끼리 분류해주세요.",
                                                          "cards": ["티셔츠", "꽃무늬가 들어간 티셔츠", "찢어진 청바지", "닥터마틴 워커"],
                                                          "categories": ["상의", "하의", "신발"]
                                                        }
                                                      ]
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "트리 테스트",
                                            summary = "TREE_TEST 등록 예시",
                                            value = """
                                                    {
                                                      "questions": [
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
                            }
                    )
            )
            @RequestBody @Valid QuestionCreateRequest request
    ) {
        QuestionCreateResponse response = questionService.createQuestions(testId, authenticatedUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("문항이 등록되었습니다.", response));
    }
}
