package server.MATE.global.discord.report;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import server.MATE.domain.report.event.ReportAggregationFailedEvent;
import server.MATE.domain.report.event.ReportAiDegradedEvent;
import server.MATE.domain.report.event.ReportReaggregationRequestedEvent;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.discord.bot.rest.DiscordBotRestClient;
import server.MATE.global.discord.channel.ErrorAlertChannel;
import server.MATE.global.discord.config.DiscordProperties;
import server.MATE.global.discord.message.DiscordMessage;
import server.MATE.global.discord.message.DiscordMessageRepository;
import server.MATE.global.discord.message.DiscordMessageType;
import server.MATE.global.discord.report.ReportAlertMessageFormatter.MessageState;
import server.MATE.global.discord.webhook.embed.DiscordEmbed;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

// 리포트 집계 알림 전송. 에러 웹훅은 원인, 리포트 관리 채널은 상태 메시지·[재집계] 버튼·처리 기록 스레드 담당
@Slf4j
@Service
public class ReportAlertService {

    private static final DiscordMessageType TYPE = DiscordMessageType.REPORT_AGGREGATION_FAILED;

    private final TestRepository testRepository;
    private final DiscordMessageRepository discordMessageRepository;
    private final DiscordBotRestClient botRestClient;
    private final ErrorAlertChannel errorAlertChannel;
    private final DiscordProperties properties;
    private final Clock clock;
    private final String deployEnv;

    public ReportAlertService(TestRepository testRepository,
                              DiscordMessageRepository discordMessageRepository,
                              DiscordBotRestClient botRestClient,
                              ErrorAlertChannel errorAlertChannel,
                              DiscordProperties properties,
                              Clock clock,
                              @Value("${deploy.env:local}") String deployEnv) {
        this.testRepository = testRepository;
        this.discordMessageRepository = discordMessageRepository;
        this.botRestClient = botRestClient;
        this.errorAlertChannel = errorAlertChannel;
        this.properties = properties;
        this.clock = clock;
        this.deployEnv = deployEnv;
    }

    public void notifyAggregationFailed(ReportAggregationFailedEvent event) {
        if (isLocal()) {
            return;
        }
        ReportAlertTarget target = loadTarget(event.testId());
        DiscordEmbed errorEmbed = ReportAlertMessageFormatter.aggregationFailedEmbed(
                target, event.cause(), event.errorMessage(), event.exception(), LocalDateTime.now(clock), deployEnv);
        errorAlertChannel.notifyBackground(errorEmbed);

        String channelId = properties.bot().reportChannelId();
        if (!canUseReportChannel(channelId) || !target.hasDetail()) {
            return;
        }
        try {
            Map<String, Object> body = ReportAlertMessageFormatter.statusMessageBody(target, event.cause(), MessageState.FAILED);
            Optional<DiscordMessage> existing = discordMessageRepository.findByTypeAndTargetId(TYPE, event.testId());
            String messageId;
            if (existing.isPresent()) {
                try {
                    messageId = existing.get().getMessageId();
                    botRestClient.editMessage(existing.get().getChannelId(), messageId, body);
                } catch (WebClientResponseException.NotFound notFound) {
                    // Discord에서 카드가 지워진 경우 위치를 정리하고 새 카드로 대체함
                    discordMessageRepository.delete(existing.get());
                    log.info("[DISCORD] 리포트 실패 카드가 없어 새로 만듭니다. testId={}", event.testId());
                    messageId = createStatusCard(channelId, event.testId(), body);
                }
            } else {
                messageId = createStatusCard(channelId, event.testId(), body);
            }
            // 메시지에서 만든 스레드 ID는 메시지 ID와 같음
            botRestClient.createMessage(messageId, ReportAlertMessageFormatter.threadEmbedBody(errorEmbed));
        } catch (RuntimeException e) {
            log.warn("[DISCORD] 리포트 실패 메시지 전송 실패. testId={}, error={}", event.testId(), e.getMessage());
        }
    }

    // 새 상태 카드 생성 및 위치 저장, 스레드 생성
    private String createStatusCard(String channelId, Long testId, Map<String, Object> body) {
        String messageId = botRestClient.createMessage(channelId, body);
        // 스레드 생성이 실패해도 상태 카드는 추적되도록 위치를 먼저 저장함
        discordMessageRepository.save(DiscordMessage.create(TYPE, testId, channelId, messageId));
        botRestClient.startThread(channelId, messageId, ReportAlertMessageFormatter.THREAD_NAME);
        return messageId;
    }

    public void notifyAiDegraded(ReportAiDegradedEvent event) {
        if (isLocal()) {
            return;
        }
        ReportAlertTarget target = loadTarget(event.testId());
        errorAlertChannel.notifyBackground(ReportAlertMessageFormatter.aiDegradedEmbed(
                target,
                event.attemptCount(),
                event.failures().stream().map(f -> Map.entry(f.questionId(), f.reason())).toList(),
                LocalDateTime.now(clock),
                deployEnv));
    }

    public void notifyAggregationCrashed(Long testId, Throwable exception) {
        if (isLocal()) {
            return;
        }
        errorAlertChannel.notifyBackground(ReportAlertMessageFormatter.aggregationCrashedEmbed(
                loadTarget(testId), exception, LocalDateTime.now(clock), deployEnv));
    }

    public void notifyReportCompleted(Long testId) {
        updateExistingMessage(testId, MessageState.COMPLETED, ReportAlertMessageFormatter.completedLine(clock.instant()));
    }

    public void notifyReaggregationRequested(ReportReaggregationRequestedEvent event) {
        updateExistingMessage(event.testId(), MessageState.REAGGREGATING,
                ReportAlertMessageFormatter.reaggregateRequestLine(event.requester(), clock.instant()));
    }

    // 이전 실패 메시지가 있을 때만 상태를 바꾸고 처리 기록 줄을 이어 씀
    private void updateExistingMessage(Long testId, MessageState state, String threadLine) {
        if (isLocal() || !botRestClient.isConfigured()) {
            return;
        }
        DiscordMessage message = null;
        try {
            Optional<DiscordMessage> existing = discordMessageRepository.findByTypeAndTargetId(TYPE, testId);
            if (existing.isEmpty()) {
                return;
            }
            message = existing.get();
            ReportAlertTarget target = loadTarget(testId);
            String cause = ReportAlertMessageFormatter.extractCause(
                    botRestClient.getMessage(message.getChannelId(), message.getMessageId()));
            botRestClient.editMessage(message.getChannelId(), message.getMessageId(),
                    ReportAlertMessageFormatter.statusMessageBody(target, cause, state));
            botRestClient.createMessage(message.getMessageId(), ReportAlertMessageFormatter.threadTextBody(threadLine));
        } catch (WebClientResponseException.NotFound e) {
            // Discord에서 카드가 지워진 경우 위치를 정리하고 새 카드로 대체함
            discordMessageRepository.delete(message);
            log.info("[DISCORD] 리포트 실패 카드가 없어 위치를 지웁니다. testId={}", testId);
        } catch (RuntimeException e) {
            log.warn("[DISCORD] 리포트 메시지 상태 갱신 실패. testId={}, state={}, error={}", testId, state, e.getMessage());
        }
    }

    // 알림 누락 방지를 위해 조회 실패 시 ID만으로 진행함
    private ReportAlertTarget loadTarget(Long testId) {
        try {
            return testRepository.findById(testId).map(ReportAlertTarget::from).orElse(ReportAlertTarget.idOnly(testId));
        } catch (RuntimeException e) {
            log.warn("[DISCORD] 리포트 알림 대상 조회 실패. testId={}, error={}", testId, e.getMessage());
            return ReportAlertTarget.idOnly(testId);
        }
    }

    private boolean canUseReportChannel(String channelId) {
        if (channelId == null || channelId.isBlank()) {
            log.warn("[DISCORD] 리포트 채널 ID가 없어 리포트 채널 전송을 건너뜁니다.");
            return false;
        }
        return botRestClient.isConfigured();
    }

    private boolean isLocal() {
        return "local".equals(deployEnv);
    }
}
