package server.MATE.global.discord.bot.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import server.MATE.domain.admin.service.AdminTestService;
import server.MATE.domain.test.dto.response.AdminTestStatusResponse;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.global.discord.bot.command.AdminTestCommands;
import server.MATE.global.discord.config.DiscordProperties;

@ExtendWith(MockitoExtension.class)
class AdminBotListenerTest {

    @Mock
    private AdminTestService adminTestService;

    @Mock
    private SlashCommandInteractionEvent event;

    @Mock
    private MessageChannelUnion channel;

    @Mock
    private ReplyCallbackAction replyCallbackAction;

    private AdminBotListener listener;

    @BeforeEach
    void setUp() {
        DiscordProperties properties = new DiscordProperties("", "", new DiscordProperties.Bot(true, "token", "123"));
        listener = new AdminBotListener(adminTestService, properties);
    }

    @Test
    @DisplayName("isAllowedChannel: 설정된 채널 id와 일치하면 true")
    void isAllowedChannel_matches() {
        assertThat(listener.isAllowedChannel("123")).isTrue();
    }

    @Test
    @DisplayName("isAllowedChannel: 설정된 채널 id와 다르면 false")
    void isAllowedChannel_mismatch() {
        assertThat(listener.isAllowedChannel("999")).isFalse();
    }

    @Test
    @DisplayName("isAllowedChannel: 설정값이 비어있으면 항상 true")
    void isAllowedChannel_blankConfigAllowsAny() {
        DiscordProperties properties = new DiscordProperties("", "", new DiscordProperties.Bot(true, "token", ""));
        AdminBotListener anyChannelListener = new AdminBotListener(adminTestService, properties);

        assertThat(anyChannelListener.isAllowedChannel("anything")).isTrue();
    }

    @Test
    @DisplayName("tests 명령이 아니면 아무 처리도 하지 않는다")
    void onSlashCommandInteraction_ignoresOtherCommands() {
        when(event.getName()).thenReturn("other");

        listener.onSlashCommandInteraction(event);

        verify(event, never()).getChannel();
        verify(event, never()).reply(anyString());
    }

    @Test
    @DisplayName("허용되지 않은 채널이면 안내 메시지만 반환하고 서비스는 호출하지 않는다")
    void onSlashCommandInteraction_blocksDisallowedChannel() {
        when(event.getName()).thenReturn(AdminTestCommands.ROOT);
        when(event.getChannel()).thenReturn(channel);
        when(channel.getId()).thenReturn("999");
        when(event.reply(AdminBotListener.CHANNEL_RESTRICTION_MESSAGE)).thenReturn(replyCallbackAction);

        listener.onSlashCommandInteraction(event);

        verify(replyCallbackAction).queue();
        verifyNoInteractions(adminTestService);
    }

    @Test
    @DisplayName("approve 서브커맨드는 AdminTestService.approve를 호출하고 결과를 응답한다")
    void onSlashCommandInteraction_approve() {
        OptionMapping testIdOption = mock(OptionMapping.class);
        when(testIdOption.getAsLong()).thenReturn(1L);

        when(event.getName()).thenReturn(AdminTestCommands.ROOT);
        when(event.getChannel()).thenReturn(channel);
        when(channel.getId()).thenReturn("123");
        when(event.getSubcommandName()).thenReturn(AdminTestCommands.SUB_APPROVE);
        when(event.getOption(AdminTestCommands.OPTION_TEST_ID)).thenReturn(testIdOption);
        when(adminTestService.approve(1L)).thenReturn(new AdminTestStatusResponse(1L, TestStatus.IN_PROGRESS));
        when(event.reply(anyString())).thenReturn(replyCallbackAction);

        listener.onSlashCommandInteraction(event);

        verify(adminTestService).approve(1L);
        verify(event).reply(eq("테스트 `#1`를 승인했습니다. (상태: `IN_PROGRESS`)"));
        verify(replyCallbackAction).queue();
    }
}
