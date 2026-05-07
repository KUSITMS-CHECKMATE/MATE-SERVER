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
import server.MATE.domain.question.dto.response.QuestionCreateResponse;
import server.MATE.domain.question.dto.response.QuestionCreateResult;
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
import static org.springframework.http.MediaType.APPLICATION_JSON;
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
                .andExpect(jsonPath("$.message").value("선택지는 필수 입력 사항입니다."));
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
