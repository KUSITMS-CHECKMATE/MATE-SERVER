package server.MATE.global.discord.bot.listener;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.MATE.domain.report.service.ReportReaggregateService;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.discord.bot.message.TeamHeartResolver;
import server.MATE.global.discord.config.DiscordProperties;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportBotListenerTest {

    @Mock private ReportReaggregateService reportReaggregateService;
    @Mock private TeamHeartResolver teamHeartResolver;
    @Mock private ButtonInteractionEvent event;
    @Mock private MessageChannelUnion channel;
    @Mock private User user;
    @Mock private ReplyCallbackAction replyAction;
    @Mock private MessageEditCallbackAction deferAction;
    @Mock private InteractionHook hook;
    @Mock private WebhookMessageCreateAction<net.dv8tion.jda.api.entities.Message> hookAction;

    private ReportBotListener listener;

    @BeforeEach
    void setUp() {
        DiscordProperties properties = new DiscordProperties("", "", new DiscordProperties.Bot(true, "t", "123", "777"), null);
        listener = new ReportBotListener(reportReaggregateService, properties, teamHeartResolver);
    }

    @Test
    @DisplayName("리포트 버튼이 아니면 무시")
    void ignoresOtherButtons() {
        given(event.getComponentId()).willReturn("admin:approve:1:WAITING");

        listener.onButtonInteraction(event);

        verifyNoInteractions(reportReaggregateService);
        verify(event, never()).getChannel();
    }

    @Test
    @DisplayName("리포트 채널이 아니면 안내만 하고 재집계하지 않음")
    void blocksOtherChannel() {
        given(event.getComponentId()).willReturn("report:reaggregate:15");
        given(event.getChannel()).willReturn(channel);
        given(channel.getId()).willReturn("123");
        given(event.reply(ReportBotListener.CHANNEL_RESTRICTION_MESSAGE)).willReturn(replyAction);
        given(replyAction.setEphemeral(true)).willReturn(replyAction);

        listener.onButtonInteraction(event);

        verify(replyAction).queue();
        verifyNoInteractions(reportReaggregateService);
    }

    @Test
    @DisplayName("리포트 채널에서 누르면 응답을 미루고 누른 사람 이름으로 재집계")
    void reaggregatesWithRequester() {
        given(event.getComponentId()).willReturn("report:reaggregate:15");
        given(event.getChannel()).willReturn(channel);
        given(channel.getId()).willReturn("777");
        given(event.getUser()).willReturn(user);
        given(user.getIdLong()).willReturn(42L);
        given(user.getEffectiveName()).willReturn("소윤");
        given(teamHeartResolver.resolve(42L)).willReturn("💙");
        given(event.deferEdit()).willReturn(deferAction);

        listener.onButtonInteraction(event);

        verify(deferAction).queue();
        verify(reportReaggregateService).reaggregate(15L, "💙 **소윤** 님이");
    }

    @Test
    @DisplayName("재집계가 거절되면 누른 사람에게만 오류를 보여줌")
    void rejectedShowsEphemeralError() {
        given(event.getComponentId()).willReturn("report:reaggregate:15");
        given(event.getChannel()).willReturn(channel);
        given(channel.getId()).willReturn("777");
        given(event.getUser()).willReturn(user);
        given(user.getIdLong()).willReturn(42L);
        given(user.getEffectiveName()).willReturn("소윤");
        given(teamHeartResolver.resolve(42L)).willReturn("💙");
        given(event.deferEdit()).willReturn(deferAction);
        given(reportReaggregateService.reaggregate(15L, "💙 **소윤** 님이")).willThrow(new BaseException(BaseErrorCode.REPORT_013));
        given(event.getHook()).willReturn(hook);
        given(hook.sendMessage(anyString())).willReturn(hookAction);
        given(hookAction.setEphemeral(true)).willReturn(hookAction);

        listener.onButtonInteraction(event);

        verify(hook).sendMessage("오류: " + BaseErrorCode.REPORT_013.getMessage());
        verify(hookAction).queue();
    }

    @Test
    @DisplayName("예상 밖 오류가 발생해도 누른 사람에게 일반 오류 안내를 보여줌")
    void unexpectedErrorShowsGenericEphemeralError() {
        given(event.getComponentId()).willReturn("report:reaggregate:15");
        given(event.getChannel()).willReturn(channel);
        given(channel.getId()).willReturn("777");
        given(event.getUser()).willReturn(user);
        given(user.getIdLong()).willReturn(42L);
        given(user.getEffectiveName()).willReturn("소윤");
        given(teamHeartResolver.resolve(42L)).willReturn("💙");
        given(event.deferEdit()).willReturn(deferAction);
        given(reportReaggregateService.reaggregate(15L, "💙 **소윤** 님이")).willThrow(new IllegalStateException("db down"));
        given(event.getHook()).willReturn(hook);
        given(hook.sendMessage(anyString())).willReturn(hookAction);
        given(hookAction.setEphemeral(true)).willReturn(hookAction);

        assertThatCode(() -> listener.onButtonInteraction(event)).doesNotThrowAnyException();

        verify(hook).sendMessage(ReportBotListener.UNEXPECTED_ERROR_MESSAGE);
        verify(hookAction).queue();
    }
}
