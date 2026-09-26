package server.MATE.global.discord.bot.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;
import server.MATE.domain.report.service.ReportReaggregateService;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.discord.bot.interaction.ReportBotCustomIds;
import server.MATE.global.discord.bot.message.TeamHeartResolver;
import server.MATE.global.discord.config.DiscordProperties;
import server.MATE.global.discord.report.ReportAlertMessageFormatter;

// 리포트 관리 채널 버튼 처리. 메시지 상태 표시는 재집계 요청 이벤트 리스너가 REST로 처리함
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportBotListener extends ListenerAdapter {

    static final String CHANNEL_RESTRICTION_MESSAGE = "이 버튼은 리포트 관리 채널에서만 사용할 수 있습니다.";
    static final String UNEXPECTED_ERROR_MESSAGE = "오류: 재집계 요청 처리 중 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.";

    private final ReportReaggregateService reportReaggregateService;
    private final DiscordProperties properties;
    private final TeamHeartResolver teamHeartResolver;

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (!ReportBotCustomIds.isReaggregate(id)) {
            return;
        }
        if (!isAllowedChannel(event.getChannel().getId())) {
            event.reply(CHANNEL_RESTRICTION_MESSAGE).setEphemeral(true).queue();
            return;
        }

        long testId = ReportBotCustomIds.parseTestId(id);
        User user = event.getUser();
        String requester = ReportAlertMessageFormatter.memberRequester(
                teamHeartResolver.resolve(user.getIdLong()), user.getEffectiveName());

        // 3초 응답 제한 대응. 메시지 수정은 요청 이벤트 리스너가 수행함
        event.deferEdit().queue();
        try {
            reportReaggregateService.reaggregate(testId, requester);
        } catch (BaseException e) {
            event.getHook().sendMessage("오류: " + e.getMessage()).setEphemeral(true).queue();
        } catch (RuntimeException e) {
            log.error("리포트 재집계 버튼 처리 실패. testId={}", testId, e);
            event.getHook().sendMessage(UNEXPECTED_ERROR_MESSAGE).setEphemeral(true).queue();
        }
    }

    // 리포트 채널 미설정 시 모든 채널 거절
    boolean isAllowedChannel(String channelId) {
        String allowed = properties.bot().reportChannelId();
        return allowed != null && !allowed.isBlank() && allowed.equals(channelId);
    }
}
