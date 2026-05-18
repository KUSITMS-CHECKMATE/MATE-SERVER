package server.MATE.domain.question.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import server.MATE.domain.auth.jwt.TokenType;
import server.MATE.domain.question.dto.response.AbTestDetailResponse;
import server.MATE.domain.question.dto.response.CardSortingDetailResponse;
import server.MATE.domain.question.dto.response.FiveSecondDetailResponse;
import server.MATE.domain.question.dto.response.FiveSecondOptionDetailResponse;
import server.MATE.domain.question.dto.response.ObjectiveOptionDetailResponse;
import server.MATE.domain.question.dto.response.ObjectiveDetailResponse;
import server.MATE.domain.question.dto.response.QuestionCreateResponse;
import server.MATE.domain.question.dto.response.QuestionCreateResult;
import server.MATE.domain.question.dto.response.QuestionDetailResponse;
import server.MATE.domain.question.dto.response.ScaleDetailResponse;
import server.MATE.domain.question.dto.response.SubjectiveDetailResponse;
import server.MATE.domain.question.dto.response.TreeTestDetailResponse;
import server.MATE.domain.question.dto.response.TreeTestNodeDetailResponse;
import server.MATE.domain.question.entity.ImageRatio;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.service.QuestionService;
import server.MATE.domain.users.entity.Role;
import server.MATE.global.common.exception.GlobalExceptionHandler;
import server.MATE.global.discord.DiscordWebhookNotifier;
import server.MATE.global.security.principal.AuthenticatedUser;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class QuestionControllerTest {

    @Mock
    private QuestionService questionService;

    @Mock
    private DiscordWebhookNotifier discordWebhookNotifier;

    @InjectMocks
    private QuestionController questionController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(questionController)
                .setControllerAdvice(new GlobalExceptionHandler(discordWebhookNotifier))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @org.junit.jupiter.api.AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("문항 목록 조회 요청을 정상 처리한다")
    void handlesQuestionListRequestSuccessfully() throws Exception {
        QuestionDetailResponse response = new QuestionDetailResponse(
                10L,
                List.of(
                        new ObjectiveDetailResponse(
                                101L,
                                101L,
                                QuestionType.OBJECTIVE,
                                1L,
                                "객관식 질문",
                                "설명",
                                false,
                                null,
                                null,
                                true,
                                List.of(
                                        new ObjectiveOptionDetailResponse(1001L, "A", null, 1),
                                        new ObjectiveOptionDetailResponse(1002L, "B", "image-b", 2)
                                )
                        )
                )
        );
        given(questionService.getQuestions(10L, 1L)).willReturn(response);

        mockMvc.perform(get("/api/v1/tests/10/questions")
                        .with(authenticationPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("문항을 조회했습니다."))
                .andExpect(jsonPath("$.data.testId").value(10))
                .andExpect(jsonPath("$.data.questions.length()").value(1))
                .andExpect(jsonPath("$.data.questions[0].questionId").value(101))
                .andExpect(jsonPath("$.data.questions[0].objectiveId").value(101))
                .andExpect(jsonPath("$.data.questions[0].type").value("OBJECTIVE"))
                .andExpect(jsonPath("$.data.questions[0].sequence").value(1))
                .andExpect(jsonPath("$.data.questions[0].isDuplicate").value(false))
                .andExpect(jsonPath("$.data.questions[0].isOther").value(true))
                .andExpect(jsonPath("$.data.questions[0].options[0].objectiveOptionId").value(1001))
                .andExpect(jsonPath("$.data.questions[0].options[1].sequence").value(2));
    }

    @Test
    @DisplayName("문항 목록 조회 응답은 타입별 상세 id 필드와 트리 노드 계약을 포함한다")
    void returnsDetailIdsAndTreeContractInQuestionListResponse() throws Exception {
        QuestionDetailResponse response = new QuestionDetailResponse(
                10L,
                List.of(
                        new ObjectiveDetailResponse(
                                101L, 100L, QuestionType.OBJECTIVE, 1L, "객관식", "설명",
                                false, null, null, true,
                                List.of(new ObjectiveOptionDetailResponse(1001L, "A", null, 1))
                        ),
                        new SubjectiveDetailResponse(102L, 200L, QuestionType.SUBJECTIVE, 2L, "주관식", "설명", "subjective-image"),
                        new FiveSecondDetailResponse(
                                103L, 300L, QuestionType.FIVE_SECOND, 3L, "5초", "설명",
                                "five-second-image", ImageRatio.RATIO_9_16, true, true, 1, 2, true,
                                List.of(new FiveSecondOptionDetailResponse(3001L, "검색창", 1))
                        ),
                        new ScaleDetailResponse(104L, 400L, QuestionType.SCALE, 4L, "척도", "설명", null, "낮음", "높음", 5),
                        new AbTestDetailResponse(105L, 500L, QuestionType.AB_TEST, 5L, "AB", "설명", "a.jpg", "b.jpg", ImageRatio.RATIO_9_16),
                        new CardSortingDetailResponse(106L, 600L, QuestionType.CARD_SORTING, 6L, "카드", "설명", List.of("A", "B", "C", "D"), List.of("cat")),
                        new TreeTestDetailResponse(
                                107L, QuestionType.TREE_TEST, 7L, "트리", "설명",
                                List.of(new TreeTestNodeDetailResponse(
                                        7001L,
                                        "마이페이지",
                                        List.of(new TreeTestNodeDetailResponse(7002L, "설정", List.of()))
                                ))
                        )
                )
        );
        given(questionService.getQuestions(10L, 1L)).willReturn(response);

        mockMvc.perform(get("/api/v1/tests/10/questions")
                        .with(authenticationPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.testId").value(10))
                .andExpect(jsonPath("$.data.questions[0].objectiveId").value(100))
                .andExpect(jsonPath("$.data.questions[0].options[0].objectiveOptionId").value(1001))
                .andExpect(jsonPath("$.data.questions[1].subjectiveId").value(200))
                .andExpect(jsonPath("$.data.questions[2].fiveSecondId").value(300))
                .andExpect(jsonPath("$.data.questions[2].imageRatio").value("9:16"))
                .andExpect(jsonPath("$.data.questions[2].options[0].fiveSecondOptionId").value(3001))
                .andExpect(jsonPath("$.data.questions[3].scaleId").value(400))
                .andExpect(jsonPath("$.data.questions[4].abTestId").value(500))
                .andExpect(jsonPath("$.data.questions[4].imageRatio").value("9:16"))
                .andExpect(jsonPath("$.data.questions[5].cardSortingId").value(600))
                .andExpect(jsonPath("$.data.questions[6].features[0].treeTestId").value(7001))
                .andExpect(jsonPath("$.data.questions[6].features[0].children[0].treeTestId").value(7002))
                .andExpect(jsonPath("$.data.questions[6].features[0].sequence").doesNotExist())
                .andExpect(jsonPath("$.data.questions[6].features[0].children[0].sequence").doesNotExist());
    }

    @Test
    @DisplayName("문항 목록 조회 응답은 null 필드와 빈 리스트를 계약대로 직렬화한다")
    void serializesNullAndEmptyFieldsAsExpectedForQuestionDetails() throws Exception {
        QuestionDetailResponse response = new QuestionDetailResponse(
                10L,
                List.of(
                        new SubjectiveDetailResponse(101L, 201L, QuestionType.SUBJECTIVE, 1L, "주관식", "설명", null),
                        new FiveSecondDetailResponse(
                                102L, 202L, QuestionType.FIVE_SECOND, 2L, "5초 주관식", "설명",
                                "five-second-image", ImageRatio.RATIO_9_16, false, null, null, null, null, List.of()
                        ),
                        new ScaleDetailResponse(103L, 203L, QuestionType.SCALE, 3L, "척도", "설명", null, null, null, 5),
                        new TreeTestDetailResponse(
                                104L, QuestionType.TREE_TEST, 4L, "트리", "설명",
                                List.of(new TreeTestNodeDetailResponse(3001L, "리프", List.of()))
                        )
                )
        );
        given(questionService.getQuestions(10L, 1L)).willReturn(response);

        mockMvc.perform(get("/api/v1/tests/10/questions")
                        .with(authenticationPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questions[0].imageKey").value((String) null))
                .andExpect(jsonPath("$.data.questions[1].isDuplicate").value((String) null))
                .andExpect(jsonPath("$.data.questions[1].imageRatio").value("9:16"))
                .andExpect(jsonPath("$.data.questions[1].minSelect").value((String) null))
                .andExpect(jsonPath("$.data.questions[1].maxSelect").value((String) null))
                .andExpect(jsonPath("$.data.questions[1].isOther").value((String) null))
                .andExpect(jsonPath("$.data.questions[1].options").isArray())
                .andExpect(jsonPath("$.data.questions[1].options.length()").value(0))
                .andExpect(jsonPath("$.data.questions[2].imageKey").value((String) null))
                .andExpect(jsonPath("$.data.questions[2].minLabel").value((String) null))
                .andExpect(jsonPath("$.data.questions[2].maxLabel").value((String) null))
                .andExpect(jsonPath("$.data.questions[3].features[0].children").isArray())
                .andExpect(jsonPath("$.data.questions[3].features[0].children.length()").value(0));
    }

    @Test
    @DisplayName("문항 목록 조회 응답은 상세 DTO 필드명을 안정적으로 유지한다")
    void usesStableResponseFieldNamesForDetailDtos() throws Exception {
        QuestionDetailResponse response = new QuestionDetailResponse(
                10L,
                List.of(
                        new ObjectiveDetailResponse(
                                101L, 201L, QuestionType.OBJECTIVE, 1L, "객관식", "설명",
                                true, 1, 2, true,
                                List.of(new ObjectiveOptionDetailResponse(1001L, "A", null, 1))
                        ),
                        new FiveSecondDetailResponse(
                                102L, 202L, QuestionType.FIVE_SECOND, 2L, "5초", "설명",
                                "five-second-image", ImageRatio.RATIO_9_16, true, true, 1, 2, true,
                                List.of(new FiveSecondOptionDetailResponse(2001L, "검색창", 1))
                        )
                )
        );
        given(questionService.getQuestions(10L, 1L)).willReturn(response);

        mockMvc.perform(get("/api/v1/tests/10/questions")
                        .with(authenticationPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questions[0].objectiveId").value(201))
                .andExpect(jsonPath("$.data.questions[0].isDuplicate").value(true))
                .andExpect(jsonPath("$.data.questions[0].isOther").value(true))
                .andExpect(jsonPath("$.data.questions[0].duplicate").doesNotExist())
                .andExpect(jsonPath("$.data.questions[0].other").doesNotExist())
                .andExpect(jsonPath("$.data.questions[1].fiveSecondId").value(202))
                .andExpect(jsonPath("$.data.questions[1].imageRatio").value("9:16"))
                .andExpect(jsonPath("$.data.questions[1].isObjective").value(true))
                .andExpect(jsonPath("$.data.questions[1].isDuplicate").value(true))
                .andExpect(jsonPath("$.data.questions[1].isOther").value(true))
                .andExpect(jsonPath("$.data.questions[1].objective").doesNotExist())
                .andExpect(jsonPath("$.data.questions[1].duplicate").doesNotExist());
    }

    @Test
    @DisplayName("통합 문항 등록 요청을 정상 처리한다")
    void handlesBulkQuestionCreateRequestSuccessfully() throws Exception {
        QuestionCreateResponse response = new QuestionCreateResponse(List.of(
                new QuestionCreateResult(101L, QuestionType.OBJECTIVE, 1L, "객관식 질문"),
                new QuestionCreateResult(102L, QuestionType.SCALE, 2L, "척도 질문")
        ));
        given(questionService.createQuestions(eq(10L), eq(1L), any())).willReturn(response);

        String requestBody = """
                {
                  "questions": [
                    {
                      "type": "OBJECTIVE",
                      "title": "객관식 질문",
                      "description": "설명",
                      "isDuplicate": false,
                      "isOther": true,
                      "options": [
                        { "content": "A", "imageKey": null },
                        { "content": "B", "imageKey": null }
                      ]
                    },
                    {
                      "type": "SCALE",
                      "title": "척도 질문",
                      "description": "설명",
                      "imageKey": null,
                      "minLabel": "낮음",
                      "maxLabel": "높음",
                      "range": 5
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/tests/10/questions")
                        .with(authenticationPrincipal())
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("201"))
                .andExpect(jsonPath("$.message").value("문항이 등록되었습니다."))
                .andExpect(jsonPath("$.data.questions.length()").value(2))
                .andExpect(jsonPath("$.data.questions[0].questionId").value(101))
                .andExpect(jsonPath("$.data.questions[0].type").value("OBJECTIVE"))
                .andExpect(jsonPath("$.data.questions[0].sequence").value(1))
                .andExpect(jsonPath("$.data.questions[1].questionId").value(102))
                .andExpect(jsonPath("$.data.questions[1].type").value("SCALE"))
                .andExpect(jsonPath("$.data.questions[1].sequence").value(2));
    }

    @Test
    @DisplayName("통합 문항 등록 요청이 검증에 실패하면 400을 반환한다")
    void returnsBadRequestWhenBulkQuestionCreateRequestValidationFails() throws Exception {
        String requestBody = """
                {
                  "questions": [
                    {
                      "type": "OBJECTIVE",
                      "title": "객관식 질문",
                      "description": "설명",
                      "isDuplicate": false,
                      "isOther": true
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/tests/10/questions")
                        .with(authenticationPrincipal())
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_002"))
                .andExpect(jsonPath("$.message").value("선택지는 필수 입력 사항입니다."))
                .andExpect(jsonPath("$.field").value("questions[0].options"));
    }

    @Test
    @DisplayName("혼합 요청 중 뒤 문항이 검증에 실패하면 전체 요청이 400으로 종료되고 서비스를 호출하지 않는다")
    void returnsBadRequestWhenLaterMixedQuestionFailsValidation() throws Exception {
        String requestBody = """
                {
                  "questions": [
                    {
                      "type": "OBJECTIVE",
                      "title": "객관식 질문",
                      "description": "설명",
                      "isDuplicate": false,
                      "isOther": true,
                      "options": [
                        { "content": "A", "imageKey": null },
                        { "content": "B", "imageKey": null }
                      ]
                    },
                    {
                      "type": "OBJECTIVE",
                      "title": "두 번째 객관식",
                      "description": "설명",
                      "isDuplicate": false,
                      "isOther": true
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/tests/10/questions")
                        .with(authenticationPrincipal())
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_002"))
                .andExpect(jsonPath("$.message").value("선택지는 필수 입력 사항입니다."))
                .andExpect(jsonPath("$.field").value("questions[1].options"));

        verifyNoInteractions(questionService);
    }

    @Test
    @DisplayName("통합 문항 등록 요청 본문 파싱에 실패하면 field 없이 400을 반환한다")
    void returnsBadRequestWithoutFieldWhenRequestBodyParsingFails() throws Exception {
        String requestBody = """
                {
                  "questions": [
                    {
                      "type": "SCALE",
                      "title": "척도 질문",
                      "description": "설명",
                      "imageKey": null,
                      "minLabel": "낮음",
                      "maxLabel": "높음",
                      "range": "five"
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/tests/10/questions")
                        .with(authenticationPrincipal())
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_003"))
                .andExpect(jsonPath("$.message").value("요청 본문을 읽을 수 없습니다."))
                .andExpect(jsonPath("$.field").doesNotExist());
    }

    @Test
    @DisplayName("혼합 요청 중 뒤 문항에 다른 타입 필드가 섞이면 전체 요청이 400으로 종료되고 서비스를 호출하지 않는다")
    void returnsBadRequestWhenLaterMixedQuestionContainsAnotherTypeField() throws Exception {
        String requestBody = """
                {
                  "questions": [
                    {
                      "type": "OBJECTIVE",
                      "title": "객관식 질문",
                      "description": "설명",
                      "isDuplicate": false,
                      "isOther": true,
                      "options": [
                        { "content": "A", "imageKey": null },
                        { "content": "B", "imageKey": null }
                      ]
                    },
                    {
                      "type": "OBJECTIVE",
                      "title": "두 번째 객관식",
                      "description": "설명",
                      "isDuplicate": false,
                      "isOther": true,
                      "options": [
                        { "content": "A", "imageKey": null },
                        { "content": "B", "imageKey": null }
                      ],
                      "range": 5
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/tests/10/questions")
                        .with(authenticationPrincipal())
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_003"))
                .andExpect(jsonPath("$.message").value("요청 본문을 읽을 수 없습니다."))
                .andExpect(jsonPath("$.field").doesNotExist());

        verifyNoInteractions(questionService);
    }

    @Test
    @DisplayName("통합 문항 등록 요청 JSON 형식이 깨지면 field 없이 COMMON_003을 반환한다")
    void returnsCommon003WhenJsonSyntaxIsMalformed() throws Exception {
        String requestBody = """
                {
                  "questions": [
                    {
                      "type": "OBJECTIVE",
                      "title": "객관식 질문",
                      "isDuplicate": false,
                      "isOther": true,
                      "options": [
                        { "content": "A", "imageKey": null },
                        { "content": "B", "imageKey": null }
                      ]
                    }
                  ]
                """;

        mockMvc.perform(post("/api/v1/tests/10/questions")
                        .with(authenticationPrincipal())
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_003"))
                .andExpect(jsonPath("$.message").value("요청 본문을 읽을 수 없습니다."))
                .andExpect(jsonPath("$.field").doesNotExist());
    }

    private UsernamePasswordAuthenticationToken authentication() {
        AuthenticatedUser user = new AuthenticatedUser(1L, Role.USER, TokenType.ACCESS);
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    private RequestPostProcessor authenticationPrincipal() {
        RequestPostProcessor delegate =
                SecurityMockMvcRequestPostProcessors.authentication(authentication());
        return request -> {
            delegate.postProcessRequest(request);
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication());
            SecurityContextHolder.setContext(context);
            request.setUserPrincipal(authentication());
            return request;
        };
    }
}
