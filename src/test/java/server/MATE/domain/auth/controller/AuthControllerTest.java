package server.MATE.domain.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import server.MATE.domain.auth.dto.response.AuthReissueResponse;
import server.MATE.domain.auth.jwt.TokenType;
import server.MATE.domain.auth.service.AuthService;
import server.MATE.domain.users.entity.Role;
import server.MATE.domain.users.entity.TossUnlinkReferrer;
import server.MATE.global.common.exception.GlobalExceptionHandler;
import server.MATE.global.discord.DiscordWebhookNotifier;
import server.MATE.global.security.principal.AuthenticatedUser;
import server.MATE.toss.service.TossLoginService;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private ObjectProvider<TossLoginService> tossLoginServiceProvider;

    @Mock
    private TossLoginService tossLoginService;

    @Mock
    private DiscordWebhookNotifier discordWebhookNotifier;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authController, "tossLoginServiceProvider", tossLoginServiceProvider);
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
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
    @DisplayName("로그아웃 요청을 정상 처리한다")
    void handlesLogoutRequestSuccessfully() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(authenticationPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("로그아웃이 완료되었습니다."))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(authService).logout(1L);
    }

    @Test
    @DisplayName("토큰 재발급 요청을 정상 처리한다")
    void handlesReissueRequestSuccessfully() throws Exception {
        given(authService.reissue("refresh-token"))
                .willReturn(new AuthReissueResponse("new-access-token", "new-refresh-token"));

        mockMvc.perform(post("/api/v1/auth/reissue")
                        .contentType("application/json")
                        .content("""
                                {
                                  "refreshToken": "refresh-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("토큰이 재발급되었습니다."))
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"));
    }

    @Test
    @DisplayName("토큰 재발급 요청에서 refreshToken이 비어 있으면 400을 반환한다")
    void returnsBadRequestWhenRefreshTokenIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/auth/reissue")
                        .contentType("application/json")
                        .content("""
                                {
                                  "refreshToken": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_002"))
                .andExpect(jsonPath("$.field").value("refreshToken"));
    }

    @Test
    @DisplayName("토스 연결 해제 요청을 정상 처리한다")
    void handlesTossUnlinkRequestSuccessfully() throws Exception {
        given(tossLoginServiceProvider.getIfAvailable()).willReturn(tossLoginService);

        mockMvc.perform(post("/api/v1/auth/toss/unlink")
                        .with(authenticationPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("토스 연결이 해제되었습니다."));

        verify(tossLoginService).unlinkCurrentUser(1L);
    }

    @Test
    @DisplayName("userKey 기준 토스 연결 해제 요청을 정상 처리한다")
    void handlesTossUnlinkByUserKeySuccessfully() throws Exception {
        given(tossLoginServiceProvider.getIfAvailable()).willReturn(tossLoginService);

        mockMvc.perform(post("/api/v1/auth/toss/unlink/by-user-key")
                        .with(adminAuthenticationPrincipal())
                        .contentType("application/json")
                        .content("""
                                {
                                  "userKey": 443731103
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("토스 연결이 해제되었습니다."));

        verify(tossLoginService).unlinkByUserKey(443731103L, TossUnlinkReferrer.UNLINK);
    }

    @Test
    @DisplayName("일반 사용자가 userKey 기준 토스 연결 해제 요청을 하면 403을 반환한다")
    void returnsForbiddenWhenNonAdminRequestsUnlinkByUserKey() throws Exception {
        mockMvc.perform(post("/api/v1/auth/toss/unlink/by-user-key")
                        .with(authenticationPrincipal())
                        .contentType("application/json")
                        .content("""
                                {
                                  "userKey": 443731103
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMON_009"));
    }

    @Test
    @DisplayName("토스 연동 상태 조회 요청을 정상 처리한다")
    void handlesTossIntegrationStatusRequestSuccessfully() throws Exception {
        given(tossLoginServiceProvider.getIfAvailable()).willReturn(tossLoginService);
        given(tossLoginService.isLinked(1L)).willReturn(true);

        mockMvc.perform(get("/api/v1/auth/toss/integration-status")
                .with(authenticationPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("토스 연동 상태를 조회했습니다."))
                .andExpect(jsonPath("$.data.isLinked").value(true));
    }

    @Test
    @DisplayName("GET 연결 해제 콜백을 정상 처리한다")
    void handlesGetCallbackSuccessfully() throws Exception {
        given(tossLoginServiceProvider.getIfAvailable()).willReturn(tossLoginService);

        mockMvc.perform(get("/api/v1/auth/toss/login/unlink/callback")
                        .header("Authorization", basicAuthHeader())
                        .param("userKey", "443731103")
                        .param("referrer", "UNLINK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(tossLoginService).validateCallbackAuthorization(basicAuthHeader());
        verify(tossLoginService).handleUnlinkCallback(443731103L, server.MATE.domain.users.entity.TossUnlinkReferrer.UNLINK);
    }

    @Test
    @DisplayName("POST 연결 해제 콜백을 정상 처리한다")
    void handlesPostCallbackSuccessfully() throws Exception {
        given(tossLoginServiceProvider.getIfAvailable()).willReturn(tossLoginService);

        mockMvc.perform(post("/api/v1/auth/toss/login/unlink/callback")
                        .header("Authorization", basicAuthHeader())
                        .contentType("application/json")
                        .content("""
                                {
                                  "userKey": 443731103,
                                  "referrer": "WITHDRAWAL_TOSS"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(tossLoginService).validateCallbackAuthorization(basicAuthHeader());
        verify(tossLoginService).handleUnlinkCallback(443731103L, server.MATE.domain.users.entity.TossUnlinkReferrer.WITHDRAWAL_TOSS);
    }

    @Test
    @DisplayName("콜백 인증이 올바르지 않으면 401을 반환한다")
    void returnsUnauthorizedWhenCallbackAuthorizationIsInvalid() throws Exception {
        given(tossLoginServiceProvider.getIfAvailable()).willReturn(tossLoginService);
        org.mockito.Mockito.doThrow(new server.MATE.global.common.exception.BaseException(server.MATE.global.common.exception.BaseErrorCode.COMMON_008))
                .when(tossLoginService)
                .validateCallbackAuthorization("Basic invalid");

        mockMvc.perform(get("/api/v1/auth/toss/login/unlink/callback")
                        .header("Authorization", "Basic invalid")
                        .param("userKey", "443731103")
                        .param("referrer", "UNLINK"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON_008"));
    }

    private UsernamePasswordAuthenticationToken authentication() {
        AuthenticatedUser user = new AuthenticatedUser(1L, Role.USER, TokenType.ACCESS);
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    private UsernamePasswordAuthenticationToken adminAuthentication() {
        AuthenticatedUser user = new AuthenticatedUser(99L, Role.ADMIN, TokenType.ACCESS);
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

    private RequestPostProcessor adminAuthenticationPrincipal() {
        RequestPostProcessor delegate =
                SecurityMockMvcRequestPostProcessors.authentication(adminAuthentication());
        return request -> {
            delegate.postProcessRequest(request);
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(adminAuthentication());
            SecurityContextHolder.setContext(context);
            request.setUserPrincipal(adminAuthentication());
            return request;
        };
    }

    private String basicAuthHeader() {
        String value = "callback-user:callback-pass";
        return "Basic " + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
