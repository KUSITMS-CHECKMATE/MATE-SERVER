package server.MATE.global.security.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.test.util.ReflectionTestUtils;
import server.MATE.domain.auth.jwt.JwtConstants;
import server.MATE.domain.auth.jwt.JwtProvider;
import server.MATE.domain.auth.jwt.TokenType;
import server.MATE.domain.users.entity.Role;
import server.MATE.domain.users.entity.Users;
import server.MATE.domain.users.repository.UsersRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.security.exception.JwtAuthenticationException;
import server.MATE.global.security.principal.AuthenticatedUser;

import jakarta.servlet.FilterChain;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private AuthenticationEntryPoint authenticationEntryPoint;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 인증 없이 다음 필터로 요청을 넘긴다")
    void passThroughWhenAuthorizationHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(authenticationEntryPoint, never()).commence(any(), any(), any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("유효한 access token이면 SecurityContext에 인증 정보를 저장한다")
    void setAuthenticationWhenAccessTokenIsValid() throws Exception {
        MockHttpServletRequest request = bearerRequest("valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Users user = Users.builder()
                .ci("ci-1")
                .name("tester")
                .role(Role.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        when(jwtProvider.parseClaims("valid-token")).thenReturn(claims(1L, TokenType.ACCESS));
        when(usersRepository.findById(1L)).thenReturn(Optional.of(user));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(AuthenticatedUser.class);
        assertThat(((AuthenticatedUser) authentication.getPrincipal()).getId()).isEqualTo(1L);
        verify(filterChain).doFilter(request, response);
        verify(authenticationEntryPoint, never()).commence(any(), any(), any());
    }

    @Test
    @DisplayName("access token이 아닌 토큰 타입이면 AUTH_003 에러로 인증을 거부한다")
    void invokeEntryPointWhenTokenTypeIsNotAccess() throws Exception {
        MockHttpServletRequest request = bearerRequest("invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtProvider.parseClaims("invalid-token")).thenReturn(claims(1L, TokenType.REFRESH));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        ArgumentCaptor<AuthenticationException> captor = ArgumentCaptor.forClass(AuthenticationException.class);
        verify(authenticationEntryPoint).commence(any(), any(), captor.capture());
        assertThat(captor.getValue()).isInstanceOf(JwtAuthenticationException.class);
        assertThat(((JwtAuthenticationException) captor.getValue()).getErrorCode()).isEqualTo(BaseErrorCode.AUTH_003);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("토큰의 사용자 정보가 존재하지 않으면 AUTH_004 에러로 인증을 거부한다")
    void invokeEntryPointWhenUserDoesNotExist() throws Exception {
        MockHttpServletRequest request = bearerRequest("missing-user-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtProvider.parseClaims("missing-user-token")).thenReturn(claims(99L, TokenType.ACCESS));
        when(usersRepository.findById(99L)).thenReturn(Optional.empty());

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        ArgumentCaptor<AuthenticationException> captor = ArgumentCaptor.forClass(AuthenticationException.class);
        verify(authenticationEntryPoint).commence(any(), any(), captor.capture());
        assertThat(((JwtAuthenticationException) captor.getValue()).getErrorCode()).isEqualTo(BaseErrorCode.AUTH_004);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("만료된 토큰이면 AUTH_002 에러로 인증을 거부한다")
    void invokeEntryPointWhenTokenIsExpired() throws Exception {
        MockHttpServletRequest request = bearerRequest("expired-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtProvider.parseClaims("expired-token")).thenThrow(new ExpiredJwtException(null, null, "expired"));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        ArgumentCaptor<AuthenticationException> captor = ArgumentCaptor.forClass(AuthenticationException.class);
        verify(authenticationEntryPoint).commence(any(), any(), captor.capture());
        assertThat(((JwtAuthenticationException) captor.getValue()).getErrorCode()).isEqualTo(BaseErrorCode.AUTH_002);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("형식이 잘못된 토큰이면 AUTH_001 에러로 인증을 거부한다")
    void invokeEntryPointWhenTokenIsMalformed() throws Exception {
        MockHttpServletRequest request = bearerRequest("malformed-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtProvider.parseClaims("malformed-token")).thenThrow(new JwtException("invalid"));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        ArgumentCaptor<AuthenticationException> captor = ArgumentCaptor.forClass(AuthenticationException.class);
        verify(authenticationEntryPoint).commence(any(), any(), captor.capture());
        assertThat(((JwtAuthenticationException) captor.getValue()).getErrorCode()).isEqualTo(BaseErrorCode.AUTH_001);
        verify(filterChain, never()).doFilter(any(), any());
    }

    private MockHttpServletRequest bearerRequest(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(JwtConstants.AUTH_HEADER, JwtConstants.TOKEN_PREFIX + token);
        return request;
    }

    private Claims claims(Long userId, TokenType tokenType) {
        Claims claims = Jwts.claims();
        claims.setSubject(String.valueOf(userId));
        claims.put("tokenType", tokenType.name());
        return claims;
    }
}
