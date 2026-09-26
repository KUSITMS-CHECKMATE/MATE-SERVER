package server.MATE.global.discord.report;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import server.MATE.domain.report.event.ReportAggregationFailedEvent;
import server.MATE.domain.report.event.ReportAiDegradedEvent;
import server.MATE.domain.report.event.ReportReaggregationRequestedEvent;
import server.MATE.domain.report.service.handler.AiFailureCollector;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.discord.bot.rest.DiscordBotRestClient;
import server.MATE.global.discord.channel.ErrorAlertChannel;
import server.MATE.global.discord.config.DiscordProperties;
import server.MATE.global.discord.message.DiscordMessage;
import server.MATE.global.discord.message.DiscordMessageRepository;
import server.MATE.global.discord.message.DiscordMessageType;
import server.MATE.global.discord.webhook.embed.DiscordEmbed;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportAlertServiceTest {

    private static final Long TEST_ID = 15L;
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-27T05:10:00Z"), ZoneId.of("UTC"));

    @Mock private TestRepository testRepository;
    @Mock private DiscordMessageRepository discordMessageRepository;
    @Mock private DiscordBotRestClient botRestClient;
    @Mock private ErrorAlertChannel errorAlertChannel;

    private server.MATE.domain.test.entity.Test test;

    @BeforeEach
    void setUp() {
        test = server.MATE.domain.test.entity.Test.builder()
                .makerId(1L).title("온보딩 화면 사용성 테스트").description("설명").goalPpl(100).reward(300)
                .closedAt(LocalDateTime.of(2026, 9, 27, 23, 59, 59)).build();
        ReflectionTestUtils.setField(test, "id", TEST_ID);
        ReflectionTestUtils.setField(test, "pplCount", 98L);
    }

    private ReportAlertService service(String env, String reportChannelId) {
        DiscordProperties properties = new DiscordProperties(
                "https://discord.test/error", "", new DiscordProperties.Bot(false, "token", "", reportChannelId), null);
        return new ReportAlertService(testRepository, discordMessageRepository, botRestClient,
                errorAlertChannel, properties, CLOCK, env);
    }

    private ReportAggregationFailedEvent failed() {
        return ReportAggregationFailedEvent.of(TEST_ID, ReportAggregationFailedEvent.RETRY_EXHAUSTED, new IllegalStateException("boom"));
    }

    @Test
    @DisplayName("첫 실패: 에러 웹훅 + 새 메시지·스레드·에러 임베드 + 위치 저장")
    void notifyAggregationFailed_firstFailure_createsMessageAndThread() {
        given(testRepository.findById(TEST_ID)).willReturn(Optional.of(test));
        given(botRestClient.isConfigured()).willReturn(true);
        given(discordMessageRepository.findByTypeAndTargetId(DiscordMessageType.REPORT_AGGREGATION_FAILED, TEST_ID))
                .willReturn(Optional.empty());
        given(botRestClient.createMessage(eq("777"), anyMap())).willReturn("555");

        service("prod", "777").notifyAggregationFailed(failed());

        verify(errorAlertChannel).notifyBackground(any(DiscordEmbed.class));
        verify(botRestClient).startThread("777", "555", ReportAlertMessageFormatter.THREAD_NAME);
        verify(botRestClient).createMessage(eq("555"), argThat(body -> body.containsKey("embeds") && !body.containsKey("components")));
        ArgumentCaptor<DiscordMessage> saved = ArgumentCaptor.forClass(DiscordMessage.class);
        verify(discordMessageRepository).save(saved.capture());
        assertThat(saved.getValue().getMessageId()).isEqualTo("555");

        InOrder inOrder = inOrder(botRestClient, discordMessageRepository);
        inOrder.verify(botRestClient).createMessage(eq("777"), anyMap());
        inOrder.verify(discordMessageRepository).save(any());
        inOrder.verify(botRestClient).startThread(eq("777"), eq("555"), any());
    }

    @Test
    @DisplayName("첫 실패: 스레드 생성이 실패해도 위치는 저장됨")
    void notifyAggregationFailed_threadFailure_stillSavesMessage() {
        given(testRepository.findById(TEST_ID)).willReturn(Optional.of(test));
        given(botRestClient.isConfigured()).willReturn(true);
        given(discordMessageRepository.findByTypeAndTargetId(DiscordMessageType.REPORT_AGGREGATION_FAILED, TEST_ID))
                .willReturn(Optional.empty());
        given(botRestClient.createMessage(eq("777"), anyMap())).willReturn("555");
        doThrow(new IllegalStateException("403")).when(botRestClient).startThread(any(), any(), any());

        assertThatCode(() -> service("prod", "777").notifyAggregationFailed(failed())).doesNotThrowAnyException();

        verify(errorAlertChannel).notifyBackground(any(DiscordEmbed.class));
        ArgumentCaptor<DiscordMessage> saved = ArgumentCaptor.forClass(DiscordMessage.class);
        verify(discordMessageRepository).save(saved.capture());
        assertThat(saved.getValue().getMessageId()).isEqualTo("555");
    }

    @Test
    @DisplayName("두 번째 실패: 기존 메시지를 실패 상태로 수정하고 같은 스레드에 이어 씀")
    void notifyAggregationFailed_existingMessage_editsAndAppendsToThread() {
        given(testRepository.findById(TEST_ID)).willReturn(Optional.of(test));
        given(botRestClient.isConfigured()).willReturn(true);
        given(discordMessageRepository.findByTypeAndTargetId(DiscordMessageType.REPORT_AGGREGATION_FAILED, TEST_ID))
                .willReturn(Optional.of(DiscordMessage.create(DiscordMessageType.REPORT_AGGREGATION_FAILED, TEST_ID, "777", "555")));

        service("prod", "777").notifyAggregationFailed(failed());

        verify(botRestClient).editMessage(eq("777"), eq("555"), argThat(body -> !((List<?>) body.get("components")).isEmpty()));
        verify(botRestClient).createMessage(eq("555"), anyMap());
        verify(botRestClient, never()).startThread(any(), any(), any());
        verify(discordMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("리포트 채널 미설정: 에러 웹훅만 보냄")
    void notifyAggregationFailed_blankChannel_sendsWebhookOnly() {
        given(testRepository.findById(TEST_ID)).willReturn(Optional.of(test));

        service("prod", "").notifyAggregationFailed(failed());

        verify(errorAlertChannel).notifyBackground(any(DiscordEmbed.class));
        verifyNoInteractions(botRestClient, discordMessageRepository);
    }

    @Test
    @DisplayName("Discord REST 실패는 밖으로 던지지 않음")
    void notifyAggregationFailed_restFailure_doesNotThrow() {
        given(testRepository.findById(TEST_ID)).willReturn(Optional.of(test));
        given(botRestClient.isConfigured()).willReturn(true);
        given(discordMessageRepository.findByTypeAndTargetId(any(), any())).willReturn(Optional.empty());
        given(botRestClient.createMessage(anyString(), anyMap())).willThrow(new IllegalStateException("429"));

        assertThatCode(() -> service("prod", "777").notifyAggregationFailed(failed())).doesNotThrowAnyException();
        verify(errorAlertChannel).notifyBackground(any(DiscordEmbed.class));
    }

    @Test
    @DisplayName("local 환경: 아무것도 보내지 않음")
    void notifyAggregationFailed_local_skipsAll() {
        service("local", "777").notifyAggregationFailed(failed());

        verifyNoInteractions(errorAlertChannel, botRestClient, discordMessageRepository);
    }

    @Test
    @DisplayName("AI 실패: 에러 웹훅만")
    void notifyAiDegraded_webhookOnly() {
        given(testRepository.findById(TEST_ID)).willReturn(Optional.of(test));

        service("prod", "777").notifyAiDegraded(new ReportAiDegradedEvent(
                TEST_ID, 5, List.of(new AiFailureCollector.AiFailure(101L, "E: x"))));

        verify(errorAlertChannel).notifyBackground(argThat(embed -> embed.title().equals("⚠️ AI 요약 실패")));
        verifyNoInteractions(botRestClient);
    }

    @Test
    @DisplayName("완료: 메시지가 있을 때만 완료 상태로 수정하고 완료 줄을 씀")
    void notifyReportCompleted() {
        given(botRestClient.isConfigured()).willReturn(true);
        given(discordMessageRepository.findByTypeAndTargetId(DiscordMessageType.REPORT_AGGREGATION_FAILED, TEST_ID))
                .willReturn(Optional.of(DiscordMessage.create(DiscordMessageType.REPORT_AGGREGATION_FAILED, TEST_ID, "777", "555")));
        given(testRepository.findById(TEST_ID)).willReturn(Optional.of(test));
        given(botRestClient.getMessage("777", "555")).willReturn(Map.of("embeds", List.of(Map.of(
                "description", "⚠️ 원인 | `재시도 3회 실패`"))));

        service("prod", "777").notifyReportCompleted(TEST_ID);

        verify(botRestClient).editMessage(eq("777"), eq("555"), anyMap());
        verify(botRestClient).createMessage("555", ReportAlertMessageFormatter.threadTextBody(
                ReportAlertMessageFormatter.completedLine(CLOCK.instant())));
    }

    @Test
    @DisplayName("완료: 한 번도 실패한 적 없으면 아무것도 안 함")
    void notifyReportCompleted_noMessage_noop() {
        given(botRestClient.isConfigured()).willReturn(true);
        given(discordMessageRepository.findByTypeAndTargetId(any(), any())).willReturn(Optional.empty());

        service("prod", "777").notifyReportCompleted(TEST_ID);

        verify(botRestClient, never()).editMessage(any(), any(), any());
    }

    @Test
    @DisplayName("재집계 요청: 재집계 중 상태로 수정하고 요청 줄을 씀")
    void notifyReaggregationRequested() {
        given(botRestClient.isConfigured()).willReturn(true);
        given(discordMessageRepository.findByTypeAndTargetId(DiscordMessageType.REPORT_AGGREGATION_FAILED, TEST_ID))
                .willReturn(Optional.of(DiscordMessage.create(DiscordMessageType.REPORT_AGGREGATION_FAILED, TEST_ID, "777", "555")));
        given(testRepository.findById(TEST_ID)).willReturn(Optional.of(test));
        given(botRestClient.getMessage("777", "555")).willReturn(Map.of());

        service("prod", "777").notifyReaggregationRequested(
                new ReportReaggregationRequestedEvent(TEST_ID, ReportAlertMessageFormatter.API_REQUESTER));

        verify(botRestClient).editMessage(eq("777"), eq("555"), argThat(body -> ((List<?>) body.get("components")).isEmpty()));
        verify(botRestClient).createMessage("555", ReportAlertMessageFormatter.threadTextBody(
                ReportAlertMessageFormatter.reaggregateRequestLine(ReportAlertMessageFormatter.API_REQUESTER, CLOCK.instant())));
    }

    @Test
    @DisplayName("recover 실패: 조회까지 실패해도 #ID로 에러 웹훅을 보냄")
    void notifyAggregationCrashed_lookupFails_sendsIdOnly() {
        given(testRepository.findById(TEST_ID)).willThrow(new IllegalStateException("db down"));

        service("prod", "777").notifyAggregationCrashed(TEST_ID, new IllegalStateException("tx"));

        verify(errorAlertChannel).notifyBackground(argThat(embed -> embed.description().contains("**테스트** : `#15`")));
    }
}
