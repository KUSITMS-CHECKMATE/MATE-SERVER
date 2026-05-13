package server.MATE.domain.answer.controller;

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
import org.springframework.web.bind.annotation.*;
import server.MATE.domain.answer.dto.request.AnswerCreateRequest;
import server.MATE.domain.answer.dto.response.AnswerBatchCreateResponse;
import server.MATE.domain.answer.service.AnswerService;
import server.MATE.global.common.response.ApiResponse;
import server.MATE.global.security.principal.AuthenticatedUser;

@Tag(name = "[ANSWER] 응답 API", description = "테스트 응답 관련 API")
@RestController
@RequestMapping("/api/v1/tests/{testId}/answers")
@SecurityRequirement(name = "JWT")
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    @Operation(summary = "테스트 응답 일괄 등록", description = """
            참여자가 테스트의 모든 문항에 대한 응답을 한 번에 제출합니다.
            - 테스트가 진행 중(IN_PROGRESS)이고 승인(ACCEPTED) 상태여야 합니다.
            - 목표 인원(goalPpl)이 초과된 경우 참여 불가합니다.
            - 이미 참여한 테스트에 중복 제출 불가합니다.
            - `answers` 배열의 각 항목은 `type` 필드로 유형을 구분합니다.
            - 모든 문항에 빠짐없이 응답해야 합니다.

            **[type]**
            - SUBJECTIVE, OBJECTIVE, FIVE_SECOND, SCALE, AB_TEST, CARD_SORTING, TREE_TEST

            **[에러 코드]**
            | 코드 | HTTP | 설명 |
            |------|------|------|
            | TEST_004 | 404 | 테스트를 찾을 수 없습니다 |
            | PARTICIPATION_002 | 400 | 참여할 수 없는 테스트입니다 |
            | PARTICIPATION_003 | 400 | 이미 참여한 테스트입니다 |
            | PARTICIPATION_004 | 400 | 테스트 참여 인원이 마감되었습니다 |
            | ANSWER_001 | 400 | 질문 타입과 응답 타입 불일치 |
            | ANSWER_003 | 400 | 동일 질문에 대한 응답이 중복되었습니다 |
            | ANSWER_005 | 400 | 응답이 입력되지 않았습니다 |
            | ANSWER_006 | 400 | 선택 개수가 허용 범위를 벗어났습니다 |
            | ANSWER_008 | 400 | 모든 문항에 응답해야 합니다 |
            """)
    @PostMapping
    public ResponseEntity<ApiResponse<AnswerBatchCreateResponse>> createAnswers(
            @PathVariable Long testId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "주관식",
                                            summary = "SUBJECTIVE 예시",
                                            value = """
                                                    {
                                                      "answers": [
                                                        {
                                                          "type": "SUBJECTIVE",
                                                          "questionId": 1,
                                                          "text": "UI가 직관적이고 사용하기 편했습니다."
                                                        }
                                                      ]
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "객관식",
                                            summary = "OBJECTIVE 예시",
                                            value = """
                                                    {
                                                      "answers": [
                                                        {
                                                          "type": "OBJECTIVE",
                                                          "questionId": 2,
                                                          "optionIds": [1001, 1002],
                                                          "otherText": null
                                                        }
                                                      ]
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "5초 테스트 (주관식)",
                                            summary = "FIVE_SECOND 주관식 모드(isObjective=false) 예시",
                                            value = """
                                                    {
                                                      "answers": [
                                                        {
                                                          "type": "FIVE_SECOND",
                                                          "questionId": 3,
                                                          "text": "가장 먼저 검색창이 눈에 들어왔습니다.",
                                                          "optionIds": null
                                                        }
                                                      ]
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "5초 테스트 (객관식)",
                                            summary = "FIVE_SECOND 객관식 모드(isObjective=true) 예시",
                                            value = """
                                                    {
                                                      "answers": [
                                                        {
                                                          "type": "FIVE_SECOND",
                                                          "questionId": 3,
                                                          "text": null,
                                                          "optionIds": [2001]
                                                        }
                                                      ]
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "척도",
                                            summary = "SCALE 예시",
                                            value = """
                                                    {
                                                      "answers": [
                                                        {
                                                          "type": "SCALE",
                                                          "questionId": 4,
                                                          "value": 4
                                                        }
                                                      ]
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "AB 테스트",
                                            summary = "AB_TEST 예시",
                                            value = """
                                                    {
                                                      "answers": [
                                                        {
                                                          "type": "AB_TEST",
                                                          "questionId": 5,
                                                          "selected": "A"
                                                        }
                                                      ]
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "카드 소팅",
                                            summary = "CARD_SORTING 예시",
                                            value = """
                                                    {
                                                      "answers": [
                                                        {
                                                          "type": "CARD_SORTING",
                                                          "questionId": 6,
                                                          "groups": [
                                                            { "category": "쇼핑", "cardNames": ["홈", "장바구니"] },
                                                            { "category": "정보", "cardNames": ["검색", "공지사항"] }
                                                          ]
                                                        }
                                                      ]
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "트리 테스트",
                                            summary = "TREE_TEST 예시",
                                            value = """
                                                    {
                                                      "answers": [
                                                        {
                                                          "type": "TREE_TEST",
                                                          "questionId": 7,
                                                          "nodeId": 301
                                                        }
                                                      ]
                                                    }
                                                    """
                                    )
                            }
                    )
            )
            @RequestBody @Valid AnswerCreateRequest request
    ) {
        AnswerBatchCreateResponse response = answerService.createAnswers(testId, authenticatedUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("응답이 등록되었습니다.", response));
    }
}
