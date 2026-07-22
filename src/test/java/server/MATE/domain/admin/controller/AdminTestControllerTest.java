package server.MATE.domain.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import server.MATE.domain.admin.service.AdminTestService;
import server.MATE.domain.test.dto.response.AdminTestDetailResponse;
import server.MATE.domain.test.dto.response.AdminTestListResponse;
import server.MATE.domain.test.dto.response.AdminTestStatusResponse;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.common.exception.GlobalExceptionHandler;
import server.MATE.global.discord.DiscordWebhookNotifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminTestControllerTest {

    @Mock
    private AdminTestService adminTestService;

    @Mock
    private DiscordWebhookNotifier discordWebhookNotifier;

    @InjectMocks
    private AdminTestController adminTestController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminTestController)
                .setControllerAdvice(new GlobalExceptionHandler(discordWebhookNotifier))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    @DisplayName("목록 조회: 쿼리 파라미터를 서비스에 그대로 전달한다")
    void listTests_passesQueryParamsToService() throws Exception {
        given(adminTestService.listTests(TestStatus.IN_PROGRESS, 2, 10))
                .willReturn(new AdminTestListResponse(2, 10, 0L, List.of()));

        mockMvc.perform(get("/api/v1/admin/tests")
                        .queryParam("status", "IN_PROGRESS")
                        .queryParam("page", "2")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(2));

        verify(adminTestService).listTests(TestStatus.IN_PROGRESS, 2, 10);
    }

    @Test
    @DisplayName("목록 조회: status 파라미터 없이도 기본값으로 동작한다")
    void listTests_worksWithoutStatusParam() throws Exception {
        given(adminTestService.listTests(null, 1, 20))
                .willReturn(new AdminTestListResponse(1, 20, 0L, List.of()));

        mockMvc.perform(get("/api/v1/admin/tests"))
                .andExpect(status().isOk());

        verify(adminTestService).listTests(null, 1, 20);
    }

    @Test
    @DisplayName("상세 조회: testId로 조회 결과를 반환한다")
    void getTest_returnsDetail() throws Exception {
        given(adminTestService.getTest(10L)).willReturn(new AdminTestDetailResponse(
                10L, "제목", "설명", 300, List.of(), List.of(),
                TestStatus.WAITING, null, LocalDateTime.now()
        ));

        mockMvc.perform(get("/api/v1/admin/tests/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.testId").value(10));
    }

    @Test
    @DisplayName("승인: approve 엔드포인트 호출 시 서비스를 호출한다")
    void approve_callsService() throws Exception {
        given(adminTestService.approve(10L))
                .willReturn(new AdminTestStatusResponse(10L, TestStatus.IN_PROGRESS));

        mockMvc.perform(patch("/api/v1/admin/tests/10/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.testStatus").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("반려: reason과 함께 reject 엔드포인트를 호출한다")
    void reject_callsServiceWithReason() throws Exception {
        given(adminTestService.reject(10L, "사유"))
                .willReturn(new AdminTestStatusResponse(10L, TestStatus.REJECTED));

        mockMvc.perform(patch("/api/v1/admin/tests/10/reject")
                        .contentType("application/json")
                        .content("{\"reason\":\"사유\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.testStatus").value("REJECTED"));

        verify(adminTestService).reject(10L, "사유");
    }

    @Test
    @DisplayName("반려: 요청 본문 없이 reject 엔드포인트를 호출해도 성공한다")
    void reject_callsServiceWithoutBody() throws Exception {
        given(adminTestService.reject(10L, null))
                .willReturn(new AdminTestStatusResponse(10L, TestStatus.REJECTED));

        mockMvc.perform(patch("/api/v1/admin/tests/10/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.testStatus").value("REJECTED"));

        verify(adminTestService).reject(10L, null);
    }

    @Test
    @DisplayName("존재하지 않는 테스트 승인 시 404를 반환한다")
    void approve_notFound_returns404() throws Exception {
        given(adminTestService.approve(999L)).willThrow(new BaseException(BaseErrorCode.TEST_004));

        mockMvc.perform(patch("/api/v1/admin/tests/999/approve"))
                .andExpect(status().isNotFound());
    }
}
