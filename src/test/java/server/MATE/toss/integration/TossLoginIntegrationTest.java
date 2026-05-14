package server.MATE.toss.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

import server.MATE.domain.auth.crypto.TokenEncryptor;
import server.MATE.domain.auth.dto.response.TossLoginResponse;
import server.MATE.domain.auth.store.TossTokenStore;
import server.MATE.domain.auth.store.UserRefreshTokenStore;
import server.MATE.domain.users.entity.TossAccount;
import server.MATE.domain.users.entity.TossUnlinkReferrer;
import server.MATE.domain.users.entity.Users;
import server.MATE.domain.users.repository.TossAccountRepository;
import server.MATE.domain.users.repository.UsersRepository;
import server.MATE.global.image.ImageService;
import server.MATE.toss.client.login.TossLoginApiClient;
import server.MATE.toss.crypto.TossUserInfoDecryptor;
import server.MATE.toss.dto.request.TossRefreshTokenRequest;
import server.MATE.toss.dto.request.TossTokenRequest;
import server.MATE.toss.dto.response.TossDecryptedUserInfo;
import server.MATE.toss.dto.response.TossLoginUserResponse;
import server.MATE.toss.dto.response.TossTokenResponse;
import server.MATE.toss.service.TossLoginService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "toss.api.enabled=true",
        "toss.callback.basic-auth-header=callback-secret",
        "toss.token.refresh-cache-ttl=600000"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TossLoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TossLoginService tossLoginService;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private TossAccountRepository tossAccountRepository;

    @Autowired
    private TokenEncryptor tokenEncryptor;

    @MockitoBean
    private TossLoginApiClient tossLoginApiClient;

    @MockitoBean
    private TossUserInfoDecryptor tossUserInfoDecryptor;

    @MockitoBean
    private TossTokenStore tossTokenStore;

    @MockitoBean
    private UserRefreshTokenStore userRefreshTokenStore;

    @MockitoBean
    private ImageService imageService;

    @AfterEach
    void tearDown() {
        tossAccountRepository.deleteAll();
        usersRepository.deleteAll();
    }

    @Test
    @DisplayName("토스 연결 해제 API는 인증 없이 호출하면 401을 반환한다")
    void returnsUnauthorizedWhenUnlinkCalledWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/auth/toss/unlink"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON_008"));
    }

    @Test
    @DisplayName("로그인 성공 시 users, toss_accounts 저장과 토큰 저장이 수행된다")
    void logsInSuccessfullyAndPersistsUserAndTossAccount() {
        TossTokenResponse tokenResponse = new TossTokenResponse("Bearer", "toss-access", "toss-refresh", 3600L, "user_ci");
        TossLoginUserResponse loginUserResponse = new TossLoginUserResponse(777L, "user_ci,name", "encrypted-name", "encrypted-ci");
        TossDecryptedUserInfo decryptedUserInfo = new TossDecryptedUserInfo(777L, "user_ci,name", "ci-1", "tester");

        when(tossLoginApiClient.generateToken(new TossTokenRequest("authorization-code", "APP"))).thenReturn(tokenResponse);
        when(tossLoginApiClient.getLoginUserInfo("toss-access")).thenReturn(loginUserResponse);
        when(tossUserInfoDecryptor.decrypt(loginUserResponse)).thenReturn(decryptedUserInfo);

        TossLoginResponse response = tossLoginService.login("authorization-code", "APP");

        Users savedUser = usersRepository.findByCi("ci-1").orElseThrow();
        TossAccount savedAccount = tossAccountRepository.findByUserId(savedUser.getId()).orElseThrow();

        assertThat(response.isNewUser()).isTrue();
        assertThat(response.meResponse().id()).isEqualTo(savedUser.getId());
        assertThat(savedAccount.getTossUserKey()).isEqualTo(777L);
        assertThat(savedAccount.isLinked()).isTrue();
        assertThat(savedAccount.getEncryptedTossRefreshToken()).isNotBlank();
        assertThat(tokenEncryptor.decrypt(savedAccount.getEncryptedTossRefreshToken())).isEqualTo("toss-refresh");

        verify(tossTokenStore).saveAccessToken(savedUser.getId(), "toss-access", 3_600_000L);
        verify(tossTokenStore).saveRefreshToken(savedUser.getId(), "toss-refresh", 600_000L);
        verify(userRefreshTokenStore).save(eq(savedUser.getId()), any(String.class), any(Long.class));
    }

    @Test
    @DisplayName("기존 사용자 재로그인 시 이름 갱신과 linked 상태 복구가 수행된다")
    void restoresLinkedStateOnRelogin() {
        Users existingUser = usersRepository.save(Users.builder()
                .ci("ci-1")
                .name("before-name")
                .build());
        TossAccount existingAccount = tossAccountRepository.save(TossAccount.builder()
                .user(existingUser)
                .tossUserKey(123L)
                .encryptedTossRefreshToken("old-refresh")
                .scope("old")
                .isLinked(false)
                .unlinkReferrer(TossUnlinkReferrer.UNLINK)
                .unlinkedAt(LocalDateTime.now().minusDays(1))
                .lastLoginAt(LocalDateTime.now().minusDays(1))
                .lastTokenRefreshedAt(LocalDateTime.now().minusDays(1))
                .build());

        TossTokenResponse tokenResponse = new TossTokenResponse("Bearer", "toss-access", "toss-refresh", 3600L, "user_ci");
        TossLoginUserResponse loginUserResponse = new TossLoginUserResponse(777L, "user_ci,name", "encrypted-name", "encrypted-ci");
        TossDecryptedUserInfo decryptedUserInfo = new TossDecryptedUserInfo(777L, "user_ci,name", "ci-1", "after-name");

        when(tossLoginApiClient.generateToken(new TossTokenRequest("authorization-code", "APP"))).thenReturn(tokenResponse);
        when(tossLoginApiClient.getLoginUserInfo("toss-access")).thenReturn(loginUserResponse);
        when(tossUserInfoDecryptor.decrypt(loginUserResponse)).thenReturn(decryptedUserInfo);

        TossLoginResponse response = tossLoginService.login("authorization-code", "APP");

        Users updatedUser = usersRepository.findById(existingUser.getId()).orElseThrow();
        TossAccount updatedAccount = tossAccountRepository.findById(existingAccount.getId()).orElseThrow();

        assertThat(response.isNewUser()).isFalse();
        assertThat(updatedUser.getName()).isEqualTo("after-name");
        assertThat(updatedAccount.getTossUserKey()).isEqualTo(777L);
        assertThat(updatedAccount.isLinked()).isTrue();
        assertThat(updatedAccount.getUnlinkReferrer()).isNull();
        assertThat(updatedAccount.getUnlinkedAt()).isNull();
    }

    @Test
    @DisplayName("unlink 성공 시 토큰 저장소 정리와 unlink 상태 반영이 수행된다")
    void unlinksCurrentUserAndClearsTokens() {
        Users user = usersRepository.save(Users.builder()
                .ci("ci-1")
                .name("tester")
                .build());
        TossAccount tossAccount = tossAccountRepository.save(TossAccount.builder()
                .user(user)
                .tossUserKey(777L)
                .encryptedTossRefreshToken(tokenEncryptor.encrypt("toss-refresh"))
                .scope("user_ci")
                .isLinked(true)
                .lastLoginAt(LocalDateTime.now().minusDays(1))
                .lastTokenRefreshedAt(LocalDateTime.now().minusDays(1))
                .build());

        when(tossTokenStore.findAccessToken(user.getId())).thenReturn(Optional.of("cached-access"));

        tossLoginService.unlinkCurrentUser(user.getId());

        TossAccount updatedAccount = tossAccountRepository.findById(tossAccount.getId()).orElseThrow();
        assertThat(updatedAccount.isLinked()).isFalse();
        assertThat(updatedAccount.getUnlinkReferrer()).isEqualTo(TossUnlinkReferrer.UNLINK);
        assertThat(updatedAccount.getUnlinkedAt()).isNotNull();
        assertThat(updatedAccount.getEncryptedTossRefreshToken()).isNull();

        verify(tossLoginApiClient).removeByAccessToken("cached-access");
        verify(tossTokenStore).deleteAccessToken(user.getId());
        verify(tossTokenStore).deleteRefreshToken(user.getId());
        verify(userRefreshTokenStore).delete(user.getId());
    }

    @Test
    @DisplayName("callback 성공 시 unlinkReferrer/unlinkedAt 반영과 토큰 정리가 수행된다")
    void handlesUnlinkCallbackAndClearsTokens() {
        Users user = usersRepository.save(Users.builder()
                .ci("ci-1")
                .name("tester")
                .build());
        TossAccount tossAccount = tossAccountRepository.save(TossAccount.builder()
                .user(user)
                .tossUserKey(777L)
                .encryptedTossRefreshToken(tokenEncryptor.encrypt("toss-refresh"))
                .scope("user_ci")
                .isLinked(true)
                .lastLoginAt(LocalDateTime.now().minusDays(1))
                .lastTokenRefreshedAt(LocalDateTime.now().minusDays(1))
                .build());

        tossLoginService.handleUnlinkCallback(777L, TossUnlinkReferrer.WITHDRAWAL_TOSS);

        TossAccount updatedAccount = tossAccountRepository.findById(tossAccount.getId()).orElseThrow();
        assertThat(updatedAccount.isLinked()).isFalse();
        assertThat(updatedAccount.getUnlinkReferrer()).isEqualTo(TossUnlinkReferrer.WITHDRAWAL_TOSS);
        assertThat(updatedAccount.getUnlinkedAt()).isNotNull();
        assertThat(updatedAccount.getEncryptedTossRefreshToken()).isNull();

        verify(tossTokenStore).deleteAccessToken(user.getId());
        verify(tossTokenStore).deleteRefreshToken(user.getId());
        verify(userRefreshTokenStore).delete(user.getId());
        verify(tossLoginApiClient, never()).removeByAccessToken(any());
    }

    @Test
    @DisplayName("Redis access token이 없으면 DB refresh token으로 재발급 후 unlink 한다")
    void refreshesUsingEncryptedDbRefreshTokenBeforeUnlink() {
        Users user = usersRepository.save(Users.builder()
                .ci("ci-1")
                .name("tester")
                .build());
        TossAccount tossAccount = tossAccountRepository.save(TossAccount.builder()
                .user(user)
                .tossUserKey(777L)
                .encryptedTossRefreshToken(tokenEncryptor.encrypt("db-refresh"))
                .scope("user_ci")
                .isLinked(true)
                .lastLoginAt(LocalDateTime.now().minusDays(1))
                .lastTokenRefreshedAt(LocalDateTime.now().minusDays(1))
                .build());

        TossTokenResponse refreshedResponse = new TossTokenResponse("Bearer", "new-access", "new-refresh", 3600L, "user_ci,name");

        when(tossTokenStore.findAccessToken(user.getId())).thenReturn(Optional.empty());
        when(tossTokenStore.findRefreshToken(user.getId())).thenReturn(Optional.empty());
        when(tossLoginApiClient.refreshToken(new TossRefreshTokenRequest("db-refresh"))).thenReturn(refreshedResponse);

        tossLoginService.unlinkCurrentUser(user.getId());

        TossAccount updatedAccount = tossAccountRepository.findById(tossAccount.getId()).orElseThrow();
        assertThat(updatedAccount.isLinked()).isFalse();
        assertThat(updatedAccount.getEncryptedTossRefreshToken()).isNull();
        assertThat(updatedAccount.getUnlinkReferrer()).isEqualTo(TossUnlinkReferrer.UNLINK);

        verify(tossLoginApiClient).refreshToken(new TossRefreshTokenRequest("db-refresh"));
        verify(tossTokenStore).saveAccessToken(user.getId(), "new-access", 3_600_000L);
        verify(tossTokenStore).saveRefreshToken(user.getId(), "new-refresh", 600_000L);
        verify(tossLoginApiClient).removeByAccessToken("new-access");
        verify(tossTokenStore).deleteAccessToken(user.getId());
        verify(tossTokenStore).deleteRefreshToken(user.getId());
        verify(userRefreshTokenStore).delete(user.getId());
    }
}
