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
import server.MATE.domain.answer.dto.response.MyAnswerResponse;
import server.MATE.domain.answer.service.AnswerService;
import server.MATE.global.common.response.ApiResponse;
import server.MATE.global.security.principal.AuthenticatedUser;

@Tag(name = "[ANSWER] 응답 API", description = "테스트 응답 관련 API")
@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "JWT")
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    @Operation(summary = "✅ 응답 전체 등록", description = """
            참여자가 테스트의 모든 문항에 대한 응답을 한 번에 제출합니다. TT01-01 화면에 해당하는 api 입니다.
            - 테스트가 진행 중(`IN_PROGRESS`) 상태여야 합니다.
            - 목표 인원 수(`goalPpl`)이 초과된 경우 참여 불가합니다.
            - 이미 참여한 테스트에 중복 제출 불가합니다.
            - 응답(`answers`) 배열의 각 항목은 질문 유형(`type`) 필드로 구분합니다.
            - 모든 문항에 빠짐없이 응답해야 합니다.

            질문 유형
            - **SUBJECTIVE**: text 필수
            - **OBJECTIVE**: optionIds 필수, 기타 선택지 선택 시 otherText
            - **FIVE_SECOND**: 주관식(text) 또는 객관식(optionIds) 모드, 기타 선택지 선택 시 otherText
            - **SCALE**: value 필수 (1 ~ range)
            - **AB_TEST**: selected 필수 ("A" 또는 "B")
            - **CARD_SORTING**: groups 필수
            - **TREE_TEST**: nodeId 필수, path 필수 (루트부터 최종 노드까지 클릭 순서), 최종 선택은 leaf node여야 함
            """)
    @PostMapping("/tests/{testId}/answers")
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
                                            summary = "SUBJECTIVE 등록 예시",
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
                                            summary = "OBJECTIVE 등록 예시",
                                            value = """
                                                    {
                                                      "answers": [
                                                        {
                                                          "type": "OBJECTIVE",
                                                          "questionId": 2,
                                                          "optionIds": [1001, 1999],
                                                          "otherText": "직접 입력한 기타 의견"
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
                                                      "answers": [
                                                        {
                                                          "type": "FIVE_SECOND",
                                                          "questionId": 3,
                                                          "text": "가장 먼저 검색창이 눈에 들어왔습니다."
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
                                                      "answers": [
                                                        {
                                                          "type": "FIVE_SECOND",
                                                          "questionId": 3,
                                                          "optionIds": [2001, 2099],
                                                          "otherText": "하단 CTA 버튼"
                                                        }
                                                      ]
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "척도",
                                            summary = "SCALE 등록 예시",
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
                                            summary = "AB_TEST 등록 예시",
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
                                            summary = "CARD_SORTING 등록 예시",
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
                                            summary = "TREE_TEST 등록 예시",
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

    @Operation(
            summary = "✅ 내 응답 목록 조회",
            description = """
                    현재 로그인한 사용자가 응답한 테스트 목록을 최신순으로 조회합니다. 참여기록20 화면에 해당하는 api 입니다.
                    - createdAt은 yyyy.MM.dd 형식으로 반환합니다.
                    - totalPromotionReward는 로그인한 사용자의 누적 프로모션 리워드 합계입니다.
                    """
    )
    @GetMapping("/answers/me")
    public ResponseEntity<ApiResponse<MyAnswerResponse>> listMyAnswers(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        MyAnswerResponse response = answerService.listMyAnswers(authenticatedUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("내 응답 목록을 조회했습니다.", response));
    }
}
