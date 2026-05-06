package server.MATE.global.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.ErrorCode;
import server.MATE.global.common.response.ErrorResponse;
import server.MATE.global.security.exception.JwtAuthenticationException;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        ErrorCode errorCode = BaseErrorCode.COMMON_008;
        if (authException instanceof JwtAuthenticationException jwtAuthenticationException) {
            errorCode = jwtAuthenticationException.getErrorCode();
        }

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(errorCode));
    }
}
