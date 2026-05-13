package server.MATE.domain.answer.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import server.MATE.domain.answer.dto.request.AnswerCreateItem;
import server.MATE.domain.answer.dto.response.AnswerCreateResponse;
import server.MATE.domain.answer.service.AnswerService;
import server.MATE.global.common.response.ApiResponse;
import server.MATE.global.security.principal.AuthenticatedUser;

@Tag(name = "[ANSWER] 응답 API", description = "테스트 응답 관련 API")
@RestController
@RequestMapping("/api/v1/participations/{participationId}/answers")
@SecurityRequirement(name = "JWT")
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    @Operation(summary = "응답 등록", description = """
            질문에 대한 응답을 등록합니다.
            - `type` 필드로 응답 유형을 구분합니다: `SUBJECTIVE`, `OBJECTIVE`, `FIVE_SECOND`, `SCALE`, `AB_TEST`, `CARD_SORTING`, `TREE_TEST`
            - questionId는 type과 일치하는 질문 타입이어야 합니다.
            - 질문은 해당 참여 세션의 테스트에 속해야 합니다.

            **SUBJECTIVE**
            - `text` 필수

            **OBJECTIVE**
            - `optionIds`: 선택지 ID 목록 (단일 선택이면 1개, 복수 선택이면 minSelect~maxSelect 범위)
            - `otherText`: isOther=true인 질문에서만 입력 가능
            - 기타만 선택하는 경우 optionIds 빈 배열, otherText 입력

            **FIVE_SECOND**
            - isObjective=false(주관식 모드): `text` 필수, optionIds 불필요
            - isObjective=true(객관식 모드): `optionIds` 필수, text 불필요

            **SCALE**
            - `score` 필수: 1 이상 척도 범위(5점 또는 7점) 이하의 정수

            **AB_TEST**
            - `selected` 필수: `"A"` 또는 `"B"`

            **CARD_SORTING**
            - `groups` 필수: 카테고리별 카드 이름(cardNames) 목록
            - 모든 카드를 빠짐없이 하나의 카테고리에 배치해야 합니다
            - 카드 중복 배치 불가, 카테고리 중복 불가

            **TREE_TEST**
            - `nodeId` 필수: 선택한 리프 노드의 ID
            - 리프 노드(자식이 없는 노드)만 선택 가능
            """)
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "응답 등록 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "success": true,
                                              "code": "201",
                                              "message": "응답이 등록되었습니다.",
                                              "data": {
                                                "answerId": 1
                                              }
                                            }
                                            """
                            )
                    )
            )
    })
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
                                              "type": "SUBJECTIVE",
                                              "questionId": 1,
                                              "text": "UI가 직관적이고 사용하기 편했습니다."
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "객관식",
                                    summary = "OBJECTIVE 예시",
                                    value = """
                                            {
                                              "type": "OBJECTIVE",
                                              "questionId": 2,
                                              "optionIds": [1001, 1002],
                                              "otherText": null
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "5초 테스트 (주관식)",
                                    summary = "FIVE_SECOND 주관식 모드(isObjective=false) 예시",
                                    value = """
                                            {
                                              "type": "FIVE_SECOND",
                                              "questionId": 3,
                                              "text": "가장 먼저 검색창이 눈에 들어왔습니다.",
                                              "optionIds": null
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "5초 테스트 (객관식)",
                                    summary = "FIVE_SECOND 객관식 모드(isObjective=true) 예시",
                                    value = """
                                            {
                                              "type": "FIVE_SECOND",
                                              "questionId": 3,
                                              "text": null,
                                              "optionIds": [2001]
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "척도",
                                    summary = "SCALE 예시",
                                    value = """
                                            {
                                              "type": "SCALE",
                                              "questionId": 4,
                                              "value": 4
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "AB 테스트",
                                    summary = "AB_TEST 예시",
                                    value = """
                                            {
                                              "type": "AB_TEST",
                                              "questionId": 5,
                                              "selected": "A"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "카드 소팅",
                                    summary = "CARD_SORTING 예시",
                                    value = """
                                            {
                                              "type": "CARD_SORTING",
                                              "questionId": 6,
                                              "groups": [
                                                { "category": "쇼핑", "cardNames": ["홈", "장바구니"] },
                                                { "category": "정보", "cardNames": ["검색", "공지사항"] }
                                              ]
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "트리 테스트",
                                    summary = "TREE_TEST 예시",
                                    value = """
                                            {
                                              "type": "TREE_TEST",
                                              "questionId": 7,
                                              "nodeId": 301
                                            }
                                            """
                            )
                    }
            )
    )
    @PostMapping
    public ResponseEntity<ApiResponse<AnswerCreateResponse>> createAnswer(
            @PathVariable Long participationId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestBody @Valid AnswerCreateItem request
    ) {
        AnswerCreateResponse response = answerService.createAnswer(participationId, authenticatedUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("응답이 등록되었습니다.", response));
    }
}
