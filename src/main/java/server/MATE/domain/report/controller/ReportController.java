package server.MATE.domain.report.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.MATE.domain.report.dto.response.ReportResponse;
import server.MATE.domain.report.dto.response.TestReportExcelDownload;
import server.MATE.domain.report.service.ReportService;
import server.MATE.domain.report.service.TestReportExcelService;
import server.MATE.global.common.response.ApiResponse;
import server.MATE.global.security.principal.AuthenticatedUser;

@Tag(name = "[REPORT] 리포트 API", description = "테스트 리포트 관련 API")
@RestController
@RequestMapping("/api/v1/tests/{testId}/report")
@SecurityRequirement(name = "JWT")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final TestReportExcelService testReportExcelService;

    @Operation(
            summary = "✔️ 리포트 전체 조회",
            description = """
                    메이커가 자신의 테스트에 대한 질문 유형별 응답 리포트를 조회합니다. MKST_02 화면에 해당하는 api 입니다.
                    - 테스트 소유자(메이커)만 조회할 수 있습니다.
                    - `testStatus`가 `COMPLETED`가 아니면 `reports`는 빈 리스트를 반환합니다.
                    - `testStatus`가 `COMPLETED`이고 `reportStatus`가 `IN_PROGRESS`이면 집계 중으로 `reports`는 빈 리스트입니다.
                    - `reportStatus`가 `COMPLETED`이면 `reports`를 반환합니다.
                    - `reports[].result` 구조는 질문 유형(`type`)마다 다릅니다. 아래 예시 응답을 참고해주세요.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            content = @Content(
                    mediaType = "application/json",
                    examples = {
                            @ExampleObject(
                                    name = "IN_PROGRESS",
                                    summary = "진행 중인 테스트 (reports 빈 리스트)",
                                    value = """
                                            {
                                              "success": true,
                                              "code": "200",
                                              "message": "리포트를 조회했습니다.",
                                              "data": {
                                                "testStatus": "IN_PROGRESS",
                                                "reportStatus": "PENDING",
                                                "questionCount": 2,
                                                "participantCount": 3,
                                                "reports": []
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "COMPLETED - SUBJECTIVE",
                                    summary = "주관식 (SUBJECTIVE) — aiSummary: AI 요약, clusters: 그룹핑된 응답 목록, texts: 앱 미리보기용 최대 15개 샘플",
                                    value = """
                                            {
                                              "success": true,
                                              "code": "200",
                                              "message": "리포트를 조회했습니다.",
                                              "data": {
                                                "testStatus": "COMPLETED",
                                                "reportStatus": "COMPLETED",
                                                "questionCount": 1,
                                                "participantCount": 5,
                                                "reports": [
                                                  {
                                                    "questionId": 1,
                                                    "sequence": 1,
                                                    "title": "서비스에서 불편한 점은?",
                                                    "type": "SUBJECTIVE",
                                                    "result": {
                                                      "aiSummary": "AI 요약 준비 중입니다.",
                                                      "clusters": [
                                                        { "representative": "버튼이 너무 작아요.", "count": 2, "responses": ["버튼이 너무 작아요.", "버튼이 너무 작아요."] },
                                                        { "representative": "로딩이 느립니다.", "count": 1, "responses": ["로딩이 느립니다."] }
                                                      ],
                                                      "texts": [
                                                        "로딩이 느립니다.",
                                                        "버튼이 너무 작아요.",
                                                        "색상 대비가 부족합니다."
                                                      ]
                                                    }
                                                  }
                                                ]
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "COMPLETED - OBJECTIVE",
                                    summary = "객관식 (OBJECTIVE) — options: 선택지별 count/ratio, isOther=true 시 aiSummary/clusters/otherTexts 포함",
                                    value = """
                                            {
                                              "success": true,
                                              "code": "200",
                                              "message": "리포트를 조회했습니다.",
                                              "data": {
                                                "testStatus": "COMPLETED",
                                                "reportStatus": "COMPLETED",
                                                "questionCount": 1,
                                                "participantCount": 10,
                                                "reports": [
                                                  {
                                                    "questionId": 2,
                                                    "sequence": 1,
                                                    "title": "자주 사용하는 기능은?",
                                                    "type": "OBJECTIVE",
                                                    "result": {
                                                      "options": [
                                                        { "optionId": 101, "content": "홈 화면", "count": 6, "ratio": 0.6 },
                                                        { "optionId": 102, "content": "검색", "count": 3, "ratio": 0.3 },
                                                        { "optionId": 103, "content": "기타", "count": 1, "ratio": 0.1 }
                                                      ],
                                                      "aiSummary": "AI 요약 준비 중입니다.",
                                                      "clusters": [
                                                        { "representative": "알림 기능을 자주 씁니다.", "count": 1, "responses": ["알림 기능을 자주 씁니다."] }
                                                      ],
                                                      "otherTexts": ["알림 기능을 자주 씁니다."]
                                                    }
                                                  }
                                                ]
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "COMPLETED - FIVE_SECOND (주관식)",
                                    summary = "5초 테스트 주관식 (FIVE_SECOND, isObjective=false) — aiSummary/clusters/texts 포함",
                                    value = """
                                            {
                                              "success": true,
                                              "code": "200",
                                              "message": "리포트를 조회했습니다.",
                                              "data": {
                                                "testStatus": "COMPLETED",
                                                "reportStatus": "COMPLETED",
                                                "questionCount": 1,
                                                "participantCount": 4,
                                                "reports": [
                                                  {
                                                    "questionId": 3,
                                                    "sequence": 1,
                                                    "title": "화면에서 가장 먼저 눈에 띈 것은?",
                                                    "type": "FIVE_SECOND",
                                                    "result": {
                                                      "aiSummary": "AI 요약 준비 중입니다.",
                                                      "clusters": [
                                                        { "representative": "상단 배너가 눈에 들어왔어요.", "count": 1, "responses": ["상단 배너가 눈에 들어왔어요."] },
                                                        { "representative": "검색창이 먼저 보였습니다.", "count": 1, "responses": ["검색창이 먼저 보였습니다."] }
                                                      ],
                                                      "texts": [
                                                        "상단 배너가 눈에 들어왔어요.",
                                                        "검색창이 먼저 보였습니다."
                                                      ]
                                                    }
                                                  }
                                                ]
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "COMPLETED - FIVE_SECOND (객관식)",
                                    summary = "5초 테스트 객관식 (FIVE_SECOND, isObjective=true) — options: 선택지별 count/ratio, isOther=true 시 aiSummary/clusters/otherTexts 포함",
                                    value = """
                                            {
                                              "success": true,
                                              "code": "200",
                                              "message": "리포트를 조회했습니다.",
                                              "data": {
                                                "testStatus": "COMPLETED",
                                                "reportStatus": "COMPLETED",
                                                "questionCount": 1,
                                                "participantCount": 6,
                                                "reports": [
                                                  {
                                                    "questionId": 4,
                                                    "sequence": 1,
                                                    "title": "어떤 요소가 먼저 보였나요?",
                                                    "type": "FIVE_SECOND",
                                                    "result": {
                                                      "options": [
                                                        { "optionId": 201, "content": "검색창", "count": 4, "ratio": 0.667 },
                                                        { "optionId": 202, "content": "배너", "count": 2, "ratio": 0.333 }
                                                      ],
                                                      "aiSummary": "AI 요약 준비 중입니다.",
                                                      "clusters": [
                                                        { "representative": "하단 버튼이요", "count": 1, "responses": ["하단 버튼이요"] }
                                                      ],
                                                      "otherTexts": ["하단 버튼이요"]
                                                    }
                                                  }
                                                ]
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "COMPLETED - SCALE",
                                    summary = "척도 (SCALE) — average: 평균, mostVoted: 최다 득표 점수, endValue: 양 끝 라벨, distribution: 점수별 응답 수",
                                    value = """
                                            {
                                              "success": true,
                                              "code": "200",
                                              "message": "리포트를 조회했습니다.",
                                              "data": {
                                                "testStatus": "COMPLETED",
                                                "reportStatus": "COMPLETED",
                                                "questionCount": 1,
                                                "participantCount": 5,
                                                "reports": [
                                                  {
                                                    "questionId": 5,
                                                    "sequence": 1,
                                                    "title": "전반적인 만족도는?",
                                                    "type": "SCALE",
                                                    "result": {
                                                      "average": 3.8,
                                                      "mostVoted": 4,
                                                      "endValue": {
                                                        "minLabel": "전혀 아니다",
                                                        "maxLabel": "매우 그렇다"
                                                      },
                                                      "distribution": [
                                                        { "score": 1, "count": 0 },
                                                        { "score": 2, "count": 1 },
                                                        { "score": 3, "count": 1 },
                                                        { "score": 4, "count": 2 },
                                                        { "score": 5, "count": 1 }
                                                      ]
                                                    }
                                                  }
                                                ]
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "COMPLETED - AB_TEST",
                                    summary = "AB 테스트 (AB_TEST) — A/B 각각 count/ratio",
                                    value = """
                                            {
                                              "success": true,
                                              "code": "200",
                                              "message": "리포트를 조회했습니다.",
                                              "data": {
                                                "testStatus": "COMPLETED",
                                                "reportStatus": "COMPLETED",
                                                "questionCount": 1,
                                                "participantCount": 8,
                                                "reports": [
                                                  {
                                                    "questionId": 6,
                                                    "sequence": 1,
                                                    "title": "어떤 디자인이 더 마음에 드시나요?",
                                                    "type": "AB_TEST",
                                                    "result": {
                                                      "A": { "count": 5, "ratio": 0.625 },
                                                      "B": { "count": 3, "ratio": 0.375 }
                                                    }
                                                  }
                                                ]
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "COMPLETED - CARD_SORTING",
                                    summary = "카드 소팅 (CARD_SORTING) — byCard: 카드별 카테고리 분류 수, byCategory: 카테고리별 카드 순위",
                                    value = """
                                            {
                                              "success": true,
                                              "code": "200",
                                              "message": "리포트를 조회했습니다.",
                                              "data": {
                                                "testStatus": "COMPLETED",
                                                "reportStatus": "COMPLETED",
                                                "questionCount": 1,
                                                "participantCount": 4,
                                                "reports": [
                                                  {
                                                    "questionId": 7,
                                                    "sequence": 1,
                                                    "title": "카드를 분류해 주세요.",
                                                    "type": "CARD_SORTING",
                                                    "result": {
                                                      "byCard": [
                                                        { "cardName": "홈", "categories": { "쇼핑": 3, "정보": 1 } },
                                                        { "cardName": "검색", "categories": { "쇼핑": 0, "정보": 4 } }
                                                      ],
                                                      "byCategory": [
                                                        {
                                                          "category": "쇼핑",
                                                          "cards": [
                                                            { "rank": 1, "cardName": "홈", "count": 3, "ratio": 0.75 },
                                                            { "rank": 2, "cardName": "검색", "count": 0, "ratio": 0.0 }
                                                          ]
                                                        },
                                                        {
                                                          "category": "정보",
                                                          "cards": [
                                                            { "rank": 1, "cardName": "검색", "count": 4, "ratio": 1.0 },
                                                            { "rank": 2, "cardName": "홈", "count": 1, "ratio": 0.25 }
                                                          ]
                                                        }
                                                      ]
                                                    }
                                                  }
                                                ]
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "COMPLETED - TREE_TEST",
                                    summary = "트리 테스트 (TREE_TEST) — nodeFrequency: 최종 선택 노드별 빈도, pathFrequency: 경로별 빈도",
                                    value = """
                                            {
                                              "success": true,
                                              "code": "200",
                                              "message": "리포트를 조회했습니다.",
                                              "data": {
                                                "testStatus": "COMPLETED",
                                                "reportStatus": "COMPLETED",
                                                "questionCount": 1,
                                                "participantCount": 6,
                                                "reports": [
                                                  {
                                                    "questionId": 8,
                                                    "sequence": 1,
                                                    "title": "고객센터를 찾아보세요.",
                                                    "type": "TREE_TEST",
                                                    "result": {
                                                      "nodeFrequency": [
                                                        { "nodeId": 12, "label": "고객센터", "count": 4, "ratio": 0.667 },
                                                        { "nodeId": 15, "label": "FAQ", "count": 2, "ratio": 0.333 }
                                                      ],
                                                      "pathFrequency": [
                                                        { "path": [1, 5, 12], "pathLabels": ["홈", "지원", "고객센터"], "count": 4 },
                                                        { "path": [1, 5, 15], "pathLabels": ["홈", "지원", "FAQ"], "count": 2 }
                                                      ]
                                                    }
                                                  }
                                                ]
                                              }
                                            }
                                            """
                            )
                    }
            )
    )
    @GetMapping
    public ResponseEntity<ApiResponse<ReportResponse>> getReport(
            @PathVariable Long testId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        ReportResponse response = reportService.getReport(testId, user.getId());
        return ResponseEntity.ok(ApiResponse.ok("리포트를 조회했습니다.", response));
    }

    @Operation(
            summary = "엑셀 보고서 다운로드",
            description = """
                    테스트 기본정보와 질문 목록을 엑셀 템플릿 형식으로 다운로드합니다.
                    - 테스트 메이커만 다운로드할 수 있습니다.
                    - 질문 목록 행 수는 등록된 질문 개수에 따라 1~20개까지 가변적으로 생성됩니다.
                    """
    )
    @GetMapping("/excel")
    public ResponseEntity<byte[]> downloadExcelReport(
            @PathVariable Long testId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        TestReportExcelDownload download = testReportExcelService.export(testId, user.getId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + download.filename() + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(download.content());
    }
}
