package server.MATE.global.discord.bot.listener;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import server.MATE.domain.admin.service.AdminTestService;
import server.MATE.domain.test.dto.response.AdminTestDetailResponse;
import server.MATE.domain.test.dto.response.AdminTestListResponse;
import server.MATE.domain.test.dto.response.AdminTestStatusResponse;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.discord.bot.command.AdminTestCommands;
import server.MATE.global.discord.bot.message.AdminBotMessageFormatter;
import server.MATE.global.discord.config.DiscordProperties;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBotListener extends ListenerAdapter {

    static final String CHANNEL_RESTRICTION_MESSAGE = "이 명령어는 관리자 커맨드 채널에서만 사용할 수 있습니다.";
    private static final int MAX_DETAIL_IMAGES = 10;

    private final AdminTestService adminTestService;
    private final DiscordProperties properties;

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!AdminTestCommands.ROOT.equals(event.getName())) {
            return;
        }
        if (!isAllowedChannel(event.getChannel().getId())) {
            event.reply(CHANNEL_RESTRICTION_MESSAGE).queue();
            return;
        }

        try {
            dispatch(event);
        } catch (BaseException e) {
            event.reply("오류: " + e.getMessage()).queue();
        }
    }

    boolean isAllowedChannel(String channelId) {
        String allowed = properties.bot().adminCommandChannelId();
        return allowed == null || allowed.isBlank() || allowed.equals(channelId);
    }

    private void dispatch(SlashCommandInteractionEvent event) {
        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            event.reply("알 수 없는 명령어입니다.").queue();
            return;
        }

        switch (subcommand) {
            case AdminTestCommands.SUB_LIST -> handleList(event);
            case AdminTestCommands.SUB_DETAIL -> handleDetail(event);
            case AdminTestCommands.SUB_APPROVE -> handleApprove(event);
            case AdminTestCommands.SUB_REJECT -> handleReject(event);
            default -> event.reply("알 수 없는 명령어입니다.").queue();
        }
    }

    private void handleList(SlashCommandInteractionEvent event) {
        TestStatus status = resolveStatus(event.getOption(AdminTestCommands.OPTION_STATUS));
        int page = resolveIntOption(event.getOption(AdminTestCommands.OPTION_PAGE), 1);
        int size = resolveIntOption(event.getOption(AdminTestCommands.OPTION_SIZE), 5);

        AdminTestListResponse response = adminTestService.listTests(status, page, size);
        event.reply(AdminBotMessageFormatter.formatList(response)).queue();
    }

    private void handleDetail(SlashCommandInteractionEvent event) {
        long testId = event.getOption(AdminTestCommands.OPTION_TEST_ID).getAsLong();
        AdminTestDetailResponse response = adminTestService.getTest(testId);

        List<MessageEmbed> imageEmbeds = response.imageUrls().stream()
                .limit(MAX_DETAIL_IMAGES)
                .map(url -> new EmbedBuilder().setImage(url).build())
                .toList();

        event.reply(AdminBotMessageFormatter.formatDetail(response)).queue(hook -> {
            if (!imageEmbeds.isEmpty()) {
                hook.sendMessageEmbeds(imageEmbeds).queue();
            }
        });
    }

    private void handleApprove(SlashCommandInteractionEvent event) {
        long testId = event.getOption(AdminTestCommands.OPTION_TEST_ID).getAsLong();
        AdminTestStatusResponse response = adminTestService.approve(testId);
        event.reply(AdminBotMessageFormatter.formatApprove(response)).queue();
    }

    private void handleReject(SlashCommandInteractionEvent event) {
        long testId = event.getOption(AdminTestCommands.OPTION_TEST_ID).getAsLong();
        OptionMapping reasonOption = event.getOption(AdminTestCommands.OPTION_REASON);
        String reason = reasonOption == null ? null : reasonOption.getAsString();

        AdminTestStatusResponse response = adminTestService.reject(testId, reason);
        event.reply(AdminBotMessageFormatter.formatReject(response)).queue();
    }

    private TestStatus resolveStatus(OptionMapping option) {
        return option == null ? null : TestStatus.valueOf(option.getAsString());
    }

    private int resolveIntOption(OptionMapping option, int defaultValue) {
        return option == null ? defaultValue : option.getAsInt();
    }
}
