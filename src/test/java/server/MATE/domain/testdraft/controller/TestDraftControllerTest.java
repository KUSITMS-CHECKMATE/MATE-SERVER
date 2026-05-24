package server.MATE.domain.testdraft.controller;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import server.MATE.domain.auth.jwt.TokenType;
import server.MATE.domain.testdraft.dto.response.MyTestDraftItem;
import server.MATE.domain.testdraft.dto.response.MyTestDraftResponse;
import server.MATE.domain.testdraft.dto.response.TestDraftResponse;
import server.MATE.domain.testdraft.entity.TestDraftStatus;
import server.MATE.domain.testdraft.service.TestDraftService;
import server.MATE.domain.users.entity.Role;
import server.MATE.global.common.exception.GlobalExceptionHandler;
import server.MATE.global.discord.DiscordWebhookNotifier;
import server.MATE.global.security.principal.AuthenticatedUser;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TestDraftControllerTest {

    @Mock
    private TestDraftService testDraftService;

    @Mock
    private DiscordWebhookNotifier discordWebhookNotifier;

    @InjectMocks
    private TestDraftController testDraftController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(testDraftController)
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
    @DisplayName("테스트 초안 생성 요청을 정상 처리한다")
    void createsDraftSuccessfully() throws Exception {
        given(testDraftService.createDraft(1L)).willReturn(sampleDraftResponse());

        mockMvc.perform(post("/api/v1/test-drafts").with(authenticationPrincipal()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("테스트 초안을 생성했습니다."))
                .andExpect(jsonPath("$.data.draftId").value(10L))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    @DisplayName("테스트 초안 상세 조회 요청을 정상 처리한다")
    void getsDraftSuccessfully() throws Exception {
        given(testDraftService.getDraft(10L, 1L)).willReturn(sampleDraftResponse());

        mockMvc.perform(get("/api/v1/test-drafts/10").with(authenticationPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("테스트 초안을 조회했습니다."))
                .andExpect(jsonPath("$.data.draftId").value(10L))
                .andExpect(jsonPath("$.data.questionsPayload.questions[0].type").value("OBJECTIVE"));
    }

    @Test
    @DisplayName("내 테스트 초안 목록 조회 요청을 정상 처리한다")
    void listsDraftsSuccessfully() throws Exception {
        given(testDraftService.listMyDrafts(1L)).willReturn(new MyTestDraftResponse(
                1,
                List.of(new MyTestDraftItem(10L, "초안 제목", TestDraftStatus.DRAFT, 100, 300, LocalDateTime.parse("2026-05-25T12:00:00")))
        ));

        mockMvc.perform(get("/api/v1/test-drafts/me").with(authenticationPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.draftCount").value(1))
                .andExpect(jsonPath("$.data.drafts[0].draftId").value(10L))
                .andExpect(jsonPath("$.data.drafts[0].status").value("DRAFT"));
    }

    @Test
    @DisplayName("테스트 초안 수정 요청을 정상 처리한다")
    void updatesDraftSuccessfully() throws Exception {
        given(testDraftService.updateDraft(eq(10L), eq(1L), any())).willReturn(sampleDraftResponse());

        String request = """
                {
                  "title": "수정된 테스트",
                  "description": "수정 설명",
                  "goalPpl": 100,
                  "reward": 300,
                  "questionsPayload": {
                    "questions": [
                      {
                        "type": "OBJECTIVE",
                        "title": "질문"
                      }
                    ]
                  }
                }
                """;

        mockMvc.perform(patch("/api/v1/test-drafts/10")
                        .with(authenticationPrincipal())
                        .contentType(APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("테스트 초안을 수정했습니다."))
                .andExpect(jsonPath("$.data.title").value("초안 제목"));
    }

    @Test
    @DisplayName("테스트 초안 삭제 요청을 정상 처리한다")
    void deletesDraftSuccessfully() throws Exception {
        mockMvc.perform(delete("/api/v1/test-drafts/10").with(authenticationPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("테스트 초안을 삭제했습니다."));

        verify(testDraftService).deleteDraft(10L, 1L);
    }

    private TestDraftResponse sampleDraftResponse() {
        JsonNode payload = objectMapper.valueToTree(
                java.util.Map.of(
                        "questions", List.of(
                                java.util.Map.of("type", "OBJECTIVE", "title", "질문 1")
                        )
                )
        );
        return new TestDraftResponse(
                10L,
                1L,
                "초안 제목",
                "초안 설명",
                "서비스",
                "서비스 설명",
                List.of("img-1"),
                List.of("FOOD"),
                100,
                300,
                payload,
                TestDraftStatus.DRAFT,
                null,
                null,
                null,
                null,
                LocalDateTime.parse("2026-05-25T11:00:00"),
                LocalDateTime.parse("2026-05-25T12:00:00")
        );
    }

    private RequestPostProcessor authenticationPrincipal() {
        return request -> {
            AuthenticatedUser principal = new AuthenticatedUser(
                    1L,
                    Role.USER,
                    TokenType.ACCESS
            );
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            return request;
        };
    }
}
