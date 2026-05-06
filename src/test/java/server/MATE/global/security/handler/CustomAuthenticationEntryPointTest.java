package server.MATE.global.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.security.exception.JwtAuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;

class CustomAuthenticationEntryPointTest {

    private final CustomAuthenticationEntryPoint entryPoint = new CustomAuthenticationEntryPoint(new ObjectMapper());

    @Test
    @DisplayName("일반 인증 예외가 들어오면 COMMON_008 인증 필요 응답을 반환한다")
    void returnCommonUnauthorizedWhenAuthenticationExceptionIsGeneric() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(
                request,
                response,
                new InsufficientAuthenticationException("authentication required")
        );

        assertThat(response.getStatus()).isEqualTo(BaseErrorCode.COMMON_008.getHttpStatus().value());
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).contains("\"code\":\"COMMON_008\"");
    }

    @Test
    @DisplayName("JWT 인증 예외가 들어오면 해당 JWT 에러코드로 응답을 반환한다")
    void returnJwtErrorCodeWhenAuthenticationExceptionIsJwtAuthenticationException() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(
                request,
                response,
                new JwtAuthenticationException(BaseErrorCode.AUTH_001)
        );

        assertThat(response.getStatus()).isEqualTo(BaseErrorCode.AUTH_001.getHttpStatus().value());
        assertThat(response.getContentAsString()).contains("\"code\":\"AUTH_001\"");
    }
}
