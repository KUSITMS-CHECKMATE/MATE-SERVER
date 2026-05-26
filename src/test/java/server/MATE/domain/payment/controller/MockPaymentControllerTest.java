package server.MATE.domain.payment.controller;

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
import server.MATE.domain.payment.dto.response.PaymentCreateResponse;
import server.MATE.domain.payment.dto.response.PaymentExecuteResponse;
import server.MATE.domain.payment.dto.response.PaymentRefundResponse;
import server.MATE.domain.payment.dto.response.PaymentStatusResponse;
import server.MATE.domain.payment.entity.PayMethod;
import server.MATE.domain.payment.entity.PayStatus;
import server.MATE.domain.payment.service.MockPaymentService;
import server.MATE.domain.users.entity.Role;
import server.MATE.global.common.exception.GlobalExceptionHandler;
import server.MATE.global.discord.DiscordWebhookNotifier;
import server.MATE.global.security.principal.AuthenticatedUser;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MockPaymentControllerTest {

    @Mock
    private MockPaymentService mockPaymentService;

    @Mock
    private DiscordWebhookNotifier discordWebhookNotifier;

    @InjectMocks
    private MockPaymentController mockPaymentController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(mockPaymentController)
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
    @DisplayName("결제 생성 요청을 정상 처리한다")
    void createsPaymentSuccessfully() throws Exception {
        given(mockPaymentService.createPayment(10L, 1L, true))
                .willReturn(new PaymentCreateResponse(20L, 10L, "order-1", 30000, "mock-pay-token", true));

        mockMvc.perform(post("/api/v1/mock/payments")
                        .with(authenticationPrincipal())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "draftId": 10,
                                  "isTestPayment": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("결제를 등록했습니다."))
                .andExpect(jsonPath("$.data.paymentId").value(20L))
                .andExpect(jsonPath("$.data.payToken").value("mock-pay-token"));
    }

    @Test
    @DisplayName("결제 실행 요청을 정상 처리한다")
    void executesPaymentSuccessfully() throws Exception {
        given(mockPaymentService.executePayment(20L, 1L))
                .willReturn(new PaymentExecuteResponse(
                        20L,
                        10L,
                        99L,
                        PayStatus.PAY_SUCCEEDED,
                        "order-1",
                        30000,
                        30000,
                        "mock-pay-token",
                        "tx-1",
                        PayMethod.TOSS_MONEY,
                        LocalDateTime.parse("2026-05-25T12:00:00")
                ));

        mockMvc.perform(post("/api/v1/mock/payments/20/execute")
                        .with(authenticationPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("결제를 실행했습니다."))
                .andExpect(jsonPath("$.data.testId").value(99L))
                .andExpect(jsonPath("$.data.payStatus").value("PAY_SUCCEEDED"))
                .andExpect(jsonPath("$.data.transactionId").value("tx-1"))
                .andExpect(jsonPath("$.data.approvedAt").isArray())
                .andExpect(jsonPath("$.data.approvedAt[0]").value(2026))
                .andExpect(jsonPath("$.data.approvedAt[1]").value(5))
                .andExpect(jsonPath("$.data.approvedAt[2]").value(25));
    }

    @Test
    @DisplayName("결제 상태 조회 요청을 정상 처리한다")
    void getsPaymentStatusSuccessfully() throws Exception {
        given(mockPaymentService.getPaymentStatus(20L, 1L))
                .willReturn(new PaymentStatusResponse(
                        20L,
                        10L,
                        null,
                        "order-1",
                        "mock-pay-token",
                        PayStatus.PAY_CREATED,
                        null,
                        30000,
                        null,
                        null,
                        null
                ));

        mockMvc.perform(get("/api/v1/mock/payments/20")
                        .with(authenticationPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("결제 상태를 조회했습니다."))
                .andExpect(jsonPath("$.data.payStatus").value("PAY_CREATED"));
    }

    @Test
    @DisplayName("결제 환불 요청을 정상 처리한다")
    void refundsPaymentSuccessfully() throws Exception {
        given(mockPaymentService.refundPayment(eq(20L), eq(1L), eq("테스트 환불")))
                .willReturn(new PaymentRefundResponse(
                        20L,
                        "refund-1",
                        30000,
                        "refund-tx-1",
                        "mock-pay-token",
                        PayStatus.REFUNDED,
                        LocalDateTime.parse("2026-05-26T09:00:00")
                ));

        mockMvc.perform(post("/api/v1/mock/payments/20/refund")
                        .with(authenticationPrincipal())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "테스트 환불"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("결제를 환불했습니다."))
                .andExpect(jsonPath("$.data.refundNo").value("refund-1"))
                .andExpect(jsonPath("$.data.payStatus").value("REFUNDED"))
                .andExpect(jsonPath("$.data.approvedAt").isArray())
                .andExpect(jsonPath("$.data.approvedAt[0]").value(2026))
                .andExpect(jsonPath("$.data.approvedAt[1]").value(5))
                .andExpect(jsonPath("$.data.approvedAt[2]").value(26));

        verify(mockPaymentService).refundPayment(20L, 1L, "테스트 환불");
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
