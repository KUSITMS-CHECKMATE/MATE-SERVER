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

    @Operation(summary = "⚠️ 테스트 응답 일괄 등록", description = """
            참여자가 테스트의 모든 문항에 대한 응답을 한 번에 제출합니다.
            - 테스트가 진행 중(`IN_PROGRESS`)이고 승인(`ACCEPTED`) 상태여야 합니다.
            - 목표 인원 수(`goalPpl`)이 초과된 경우 참여 불가합니다.
            - 이미 참여한 테스트에 중복 제출 불가합니다.
            - 응답(`answers`) 배열의 각 항목은 질문 유형(`type`) 필드로 구분합니다.
            - 모든 문항에 빠짐없이 응답해야 합니다.
            - **5초 테스트 객관식 문항은 리팩토링 중 입니다. (기타(직접입력) 여부 필드 추가 필요)**

            질문 유형
            - SUBJECTIVE: text 필수
            - OBJECTIVE: optionIds 필수, 기타 선택 시 otherText
            - FIVE_SECOND: 주관식(text) 또는 객관식(optionIds) 모드
            - SCALE: value 필수 (1 ~ range)
            - AB_TEST: selected 필수 ("A" 또는 "B")
            - CARD_SORTING: groups 필수
            - TREE_TEST: nodeId 필수, path 필수 (루트부터 최종 노드까지 클릭 순서)
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
                                                          "nodeId": 301,
                                                          "path": [1, 45, 120, 301]
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
