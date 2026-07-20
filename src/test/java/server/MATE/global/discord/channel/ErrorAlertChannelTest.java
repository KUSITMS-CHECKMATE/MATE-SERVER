package server.MATE.global.discord.channel;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.discord.config.DiscordProperties;
import server.MATE.global.discord.webhook.DiscordWebhookClient;
import server.MATE.global.discord.webhook.embed.DiscordEmbed;

@ExtendWith(MockitoExtension.class)
class ErrorAlertChannelTest {

    @Mock
    private DiscordWebhookClient webhookClient;

    @Mock
    private HttpServletRequest request;

    private DiscordProperties properties;

    @BeforeEach
    void setUp() {
        properties = new DiscordProperties("https://discord.test/error", "", null);
    }

    @Test
    @DisplayName("deploy.env가 local이면 알림을 전송하지 않는다")
    void notifyError_skipsWhenLocal() {
        ErrorAlertChannel channel = new ErrorAlertChannel(webhookClient, properties, "local");

        channel.notifyError(BaseErrorCode.COMMON_999, HttpStatus.INTERNAL_SERVER_ERROR, new RuntimeException("boom"), request);

        verify(webhookClient, never()).send(anyString(), any());
    }

    @Test
    @DisplayName("local이 아닌 환경에서는 error-webhook-url로 임베드를 전송한다")
    void notifyError_sendsEmbedWhenNotLocal() {
        ErrorAlertChannel channel = new ErrorAlertChannel(webhookClient, properties, "prod");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/tests");

        channel.notifyError(BaseErrorCode.COMMON_999, HttpStatus.INTERNAL_SERVER_ERROR, new RuntimeException("boom"), request);

        verify(webhookClient).send(eq("https://discord.test/error"), any(DiscordEmbed.class));
    }
}
