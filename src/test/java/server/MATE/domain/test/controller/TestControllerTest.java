package server.MATE.domain.test.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
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
import server.MATE.domain.test.dto.request.TestDeleteMode;
import server.MATE.domain.test.service.TestDeleteService;
import server.MATE.domain.test.service.TestService;
import server.MATE.domain.users.entity.Role;
import server.MATE.global.common.exception.GlobalExceptionHandler;
import server.MATE.global.discord.DiscordWebhookNotifier;
import server.MATE.global.security.principal.AuthenticatedUser;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TestControllerTest {

    @Mock
    private TestService testService;

    @Mock
    private TestDeleteService testDeleteService;

    @Mock
    private DiscordWebhookNotifier discordWebhookNotifier;

    @InjectMocks
    private TestController testController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(testController)
                .setControllerAdvice(new GlobalExceptionHandler(discordWebhookNotifier))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("관리자 soft delete 요청을 정상 처리한다")
    void deletesTestSoftSuccessfully() throws Exception {
        mockMvc.perform(delete("/api/v1/tests/10")
                        .with(authenticationPrincipal(1L, Role.ADMIN))
                        .queryParam("mode", "SOFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("테스트를 삭제했습니다."));

        verify(testDeleteService).deleteTest(10L, Role.ADMIN, TestDeleteMode.SOFT, null);
    }

    @Test
    @DisplayName("관리자 hard delete 요청에 검증 키를 전달한다")
    void deletesTestHardSuccessfully() throws Exception {
        mockMvc.perform(delete("/api/v1/tests/10")
                        .with(authenticationPrincipal(1L, Role.ADMIN))
                        .queryParam("mode", "HARD")
                        .header("X-MATE-Hard-Delete-Key", "hard-delete-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("테스트를 삭제했습니다."));

        verify(testDeleteService).deleteTest(10L, Role.ADMIN, TestDeleteMode.HARD, "hard-delete-key");
    }

    private RequestPostProcessor authenticationPrincipal(Long userId, Role role) {
        return request -> {
            AuthenticatedUser principal = new AuthenticatedUser(userId, role, TokenType.ACCESS);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            return request;
        };
    }
}
