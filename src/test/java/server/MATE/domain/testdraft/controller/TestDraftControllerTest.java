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
import server.MATE.global.discord.channel.ErrorAlertChannel;
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
    private ErrorAlertChannel errorAlertChannel;

    @InjectMocks
    private TestDraftController testDraftController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(testDraftController)
                .setControllerAdvice(new GlobalExceptionHandler(errorAlertChannel))
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
                .andExpect(jsonPath("$.message").value("테스트 초안을 등록했습니다."))
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
                List.of(new MyTestDraftItem(
                        10L,
                        "초안 제목",
                        TestDraftStatus.DRAFT,
                        100,
                        300,
                        LocalDateTime.parse("2099-05-31T23:59:59"),
                        LocalDateTime.parse("2026-05-25T12:00:00")
                ))
        ));

        mockMvc.perform(get("/api/v1/test-drafts/me").with(authenticationPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.draftCount").value(1))
                .andExpect(jsonPath("$.data.drafts[0].draftId").value(10L))
                .andExpect(jsonPath("$.data.drafts[0].status").value("DRAFT"))
                .andExpect(jsonPath("$.data.drafts[0].closedAt[0]").value(2099))
                .andExpect(jsonPath("$.data.drafts[0].closedAt[1]").value(5))
                .andExpect(jsonPath("$.data.drafts[0].closedAt[2]").value(31));
    }

    @Test
    @DisplayName("테스트 초안 수정 요청을 정상 처리한다")
    void updatesDraftSuccessfully() throws Exception {
        given(testDraftService.updateDraft(eq(10L), eq(1L), any())).willReturn(sampleDraftResponse("수정된 테스트"));

        String request = """
                {
                  "title": "수정된 테스트",
                  "description": "수정 설명",
                  "goalPpl": 100,
                  "reward": 300,
                  "closedAt": "2099-05-31",
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
                .andExpect(jsonPath("$.data.title").value("수정된 테스트"))
                .andExpect(jsonPath("$.data.closedAt[0]").value(2099))
                .andExpect(jsonPath("$.data.closedAt[1]").value(5))
                .andExpect(jsonPath("$.data.closedAt[2]").value(31));
    }

    @Test
    @DisplayName("closedAt 형식이 yyyy-MM-dd가 아니면 검증 에러를 반환한다")
    void rejectsInvalidClosedAtFormat() throws Exception {
        String request = """
                {
                  "closedAt": "2099-05-31-23"
                }
                """;

        mockMvc.perform(patch("/api/v1/test-drafts/10")
                        .with(authenticationPrincipal())
                        .contentType(APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_002"))
                .andExpect(jsonPath("$.field").value("closedAt"))
                .andExpect(jsonPath("$.message").value("마감 기한은 yyyy-MM-dd 형식이어야 합니다."));
    }

    @Test
    @DisplayName("closedAt이 오늘이나 과거 날짜면 검증 에러를 반환한다")
    void rejectsTodayOrPastClosedAt() throws Exception {
        String request = """
                {
                  "closedAt": "2000-01-01"
                }
                """;

        mockMvc.perform(patch("/api/v1/test-drafts/10")
                        .with(authenticationPrincipal())
                        .contentType(APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_002"))
                .andExpect(jsonPath("$.field").value("closedAt"))
                .andExpect(jsonPath("$.message").value("마감 기한은 오늘 이후 날짜여야 합니다."));
    }

    @Test
    @DisplayName("테스트 초안 삭제 요청을 정상 처리한다")
    void deletesDraftSuccessfully() throws Exception {
        mockMvc.perform(delete("/api/v1/test-drafts/10").with(authenticationPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("테스트 초안을 삭제했습니다."));

        verify(testDraftService).deleteDraft(10L, 1L);
    }

    @Test
    @DisplayName("발행 가능한 초안이면 publish-check가 200을 반환한다")
    void publishCheckReturnsOkWhenDraftIsPublishable() throws Exception {
        mockMvc.perform(get("/api/v1/test-drafts/10/publish-check").with(authenticationPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("결제를 진행해도 되는 상태입니다."));

        verify(testDraftService).publishCheck(10L, 1L);
    }

    @Test
    @DisplayName("발행 불가능한 초안이면 publish-check가 해당 에러코드로 400을 반환한다")
    void publishCheckReturnsBadRequestWhenDraftIsNotPublishable() throws Exception {
        org.mockito.Mockito.doThrow(new server.MATE.global.common.exception.BaseException(
                        server.MATE.global.common.exception.BaseErrorCode.DRAFT_007))
                .when(testDraftService).publishCheck(10L, 1L);

        mockMvc.perform(get("/api/v1/test-drafts/10/publish-check").with(authenticationPrincipal()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DRAFT_007"));
    }

    private TestDraftResponse sampleDraftResponse() {
        return sampleDraftResponse("초안 제목");
    }

    private TestDraftResponse sampleDraftResponse(String title) {
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
                title,
                "초안 설명",
                "서비스",
                "서비스 설명",
                List.of("img-1"),
                List.of("FOOD"),
                100,
                300,
                LocalDateTime.parse("2099-05-31T23:59:59"),
                payload,
                TestDraftStatus.DRAFT,
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
