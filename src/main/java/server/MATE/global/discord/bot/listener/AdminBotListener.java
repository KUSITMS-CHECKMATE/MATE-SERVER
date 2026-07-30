package server.MATE.global.discord.bot.listener;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.utils.FileUpload;
import server.MATE.domain.admin.service.AdminTestService;
import server.MATE.domain.test.dto.response.AdminTestDetailResponse;
import server.MATE.domain.test.dto.response.AdminTestListResponse;
import server.MATE.domain.test.dto.response.AdminTestStatusResponse;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.discord.bot.command.AdminTestCommands;
import server.MATE.global.discord.bot.interaction.AdminBotCustomIds;
import server.MATE.global.discord.bot.message.AdminBotMessageFormatter;
import server.MATE.global.discord.bot.message.TeamHeartResolver;
import server.MATE.global.discord.config.DiscordProperties;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBotListener extends ListenerAdapter {

    static final String CHANNEL_RESTRICTION_MESSAGE = "이 명령어는 관리자 커맨드 채널에서만 사용할 수 있습니다.";
    static final String THREAD_IMAGES = "이미지 첨부";
    static final String THREAD_RECORD = "처리 기록";
    private static final int MAX_IMAGES = 10;
    private static final int THREAD_IMAGE_MIN = 2; // 2장 이상일 때만 스레드 생성

    private final AdminTestService adminTestService;
    private final DiscordProperties properties;
    private final TeamHeartResolver teamHeartResolver;

    // 슬래시 커맨드
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!AdminTestCommands.ROOT.equals(event.getName())) {
            return;
        }
        if (!isAllowedChannel(event.getChannel().getId())) {
            event.reply(CHANNEL_RESTRICTION_MESSAGE).setEphemeral(true).queue();
            return;
        }
        try {
            dispatchSlash(event);
        } catch (BaseException e) {
            replyError(event, e);
        }
    }

    private void dispatchSlash(SlashCommandInteractionEvent event) {
        String sub = event.getSubcommandName();
        if (sub == null) {
            event.reply("알 수 없는 명령어입니다.").setEphemeral(true).queue();
            return;
        }
        switch (sub) {
            case AdminTestCommands.SUB_LIST -> handleList(event);
            case AdminTestCommands.SUB_DETAIL -> handleDetail(event);
            case AdminTestCommands.SUB_APPROVE -> handleApproveCommand(event);
            case AdminTestCommands.SUB_REJECT -> handleRejectCommand(event);
            default -> event.reply("알 수 없는 명령어입니다.").setEphemeral(true).queue();
        }
    }

    private void handleList(SlashCommandInteractionEvent event) {
        TestStatus status = resolveStatus(event.getOption(AdminTestCommands.OPTION_STATUS));
        int page = resolveInt(event.getOption(AdminTestCommands.OPTION_PAGE), 1);
        int size = resolveInt(event.getOption(AdminTestCommands.OPTION_SIZE), 5);
        TestStatus effective = status == null ? TestStatus.WAITING : status;

        AdminTestListResponse response = adminTestService.listTests(status, page, size);
        event.reply(AdminBotMessageFormatter.HEADER_LIST)
                .addEmbeds(AdminBotMessageFormatter.listEmbed(effective, response))
                .addComponents(AdminBotMessageFormatter.pageButtons(effective, response))
                .queue();
    }

    private void handleDetail(SlashCommandInteractionEvent event) {
        long testId = event.getOption(AdminTestCommands.OPTION_TEST_ID).getAsLong();
        AdminTestDetailResponse detail = adminTestService.getTest(testId);

        event.reply(AdminBotMessageFormatter.HEADER_DETAIL)
                .addEmbeds(AdminBotMessageFormatter.detailEmbed(detail))
                .addComponents(AdminBotMessageFormatter.actionButtons(testId, detail.testStatus()))
                .queue(hook -> hook.retrieveOriginal().queue(msg -> attachImageThread(msg, detail)));
    }

    private void handleApproveCommand(SlashCommandInteractionEvent event) {
        long testId = event.getOption(AdminTestCommands.OPTION_TEST_ID).getAsLong();
        transition(event, testId, true, null);
    }

    private void handleRejectCommand(SlashCommandInteractionEvent event) {
        long testId = event.getOption(AdminTestCommands.OPTION_TEST_ID).getAsLong();
        var reasonOption = event.getOption(AdminTestCommands.OPTION_REASON);
        transition(event, testId, false, reasonOption == null ? null : reasonOption.getAsString());
    }

    // 버튼
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (!AdminBotCustomIds.isAdmin(id)) {
            return;
        }
        if (!isAllowedChannel(event.getChannel().getId())) {
            event.reply(CHANNEL_RESTRICTION_MESSAGE).setEphemeral(true).queue();
            return;
        }
        AdminBotCustomIds.Parsed parsed = AdminBotCustomIds.parse(id);
        try {
            switch (parsed.action()) {
                case AdminBotCustomIds.APPROVE -> transitionFromButton(event, parsed.testId(), true, null);
                case AdminBotCustomIds.REJECT -> event.replyModal(rejectModal(parsed.testId(), parsed.fromStatus())).queue();
                case AdminBotCustomIds.LIST -> handlePage(event, parsed.listStatus(), parsed.targetPage());
                default -> event.reply("알 수 없는 동작입니다.").setEphemeral(true).queue();
            }
        } catch (BaseException e) {
            replyError(event, e);
        }
    }

    private void handlePage(ButtonInteractionEvent event, TestStatus status, int targetPage) {
        AdminTestListResponse response = adminTestService.listTests(status, targetPage, 5);
        event.editMessageEmbeds(AdminBotMessageFormatter.listEmbed(status, response))
                .setComponents(AdminBotMessageFormatter.pageButtons(status, response))
                .queue();
    }

    // 모달
    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String id = event.getModalId();
        if (!AdminBotCustomIds.isAdmin(id)) {
            return;
        }
        AdminBotCustomIds.Parsed parsed = AdminBotCustomIds.parse(id);
        if (!AdminBotCustomIds.REJECT_MODAL.equals(parsed.action())) {
            return;
        }
        String reason = value(event, AdminBotCustomIds.REASON_INPUT);
        try {
            transitionFromModal(event, parsed.testId(), reason);
        } catch (BaseException e) {
            replyError(event, e);
        }
    }

    private Modal rejectModal(long testId, TestStatus from) {
        TextInput reason = TextInput.create(AdminBotCustomIds.REASON_INPUT, TextInputStyle.PARAGRAPH)
                .setPlaceholder("반려 사유를 입력하세요. 비워두면 사유 없이 반려됩니다.")
                .setRequired(false)
                .setMaxLength(500)
                .build();
        return Modal.create(AdminBotCustomIds.rejectModal(testId, from), "테스트 반려")
                .addComponents(Label.of("반려 사유 (선택)", reason))
                .build();
    }

    // 테스트 승인/ 반려

    /** 슬래시 커맨드 경로 */
    private void transition(SlashCommandInteractionEvent event, long testId, boolean approve, String reason) {
        Outcome outcome = applyTransition(testId, approve, reason);
        String header = approve ? AdminBotMessageFormatter.HEADER_APPROVE : AdminBotMessageFormatter.HEADER_REJECT;
        event.reply(header)
                .addEmbeds(AdminBotMessageFormatter.resultEmbed(outcome.detail()))
                .addComponents(AdminBotMessageFormatter.actionButtons(testId, outcome.to()))
                .queue(hook -> hook.retrieveOriginal().queue(msg -> recordProcess(msg, event.getUser(), outcome, reason)));
    }

    /** 버튼 승인 경로 (모달로 반려 사유 입력) */
    private void transitionFromButton(ButtonInteractionEvent event, long testId, boolean approve, String reason) {
        Outcome outcome = applyTransition(testId, approve, reason);
        event.editMessageEmbeds(AdminBotMessageFormatter.resultEmbed(outcome.detail()))
                .setComponents(AdminBotMessageFormatter.actionButtons(testId, outcome.to()))
                .queue(hook -> hook.retrieveOriginal().queue(msg -> recordProcess(msg, event.getUser(), outcome, reason)));
    }

    /** 모달 제출 경로 */
    private void transitionFromModal(ModalInteractionEvent event, long testId, String reason) {
        Outcome outcome = applyTransition(testId, false, reason);
        event.editMessageEmbeds(AdminBotMessageFormatter.resultEmbed(outcome.detail()))
                .setComponents(AdminBotMessageFormatter.actionButtons(testId, outcome.to()))
                .queue(hook -> hook.retrieveOriginal().queue(msg -> recordProcess(msg, event.getUser(), outcome, reason)));
    }

    private Outcome applyTransition(long testId, boolean approve, String reason) {
        TestStatus from = adminTestService.getTest(testId).testStatus();
        AdminTestStatusResponse status = approve
                ? adminTestService.approve(testId)
                : adminTestService.reject(testId, reason);
        AdminTestDetailResponse detail = adminTestService.getTest(testId);
        return new Outcome(from, status.testStatus(), detail);
    }

    private record Outcome(TestStatus from, TestStatus to, AdminTestDetailResponse detail) {
    }

    // 스레드
    private void attachImageThread(Message message, AdminTestDetailResponse detail) {
        List<String> urls = detail.imageUrls();
        if (urls == null || urls.size() < THREAD_IMAGE_MIN) {
            return;
        }
        String name = String.format("#%d %s · 이미지 %d장", detail.testId(), detail.title(), urls.size());
        message.createThreadChannel(name).queue(thread -> {
            int limit = Math.min(urls.size(), MAX_IMAGES);
            for (int i = 0; i < limit; i++) {
                try {
                    byte[] bytes = URI.create(urls.get(i)).toURL().openStream().readAllBytes();
                    thread.sendFiles(FileUpload.fromData(bytes, "image_" + (i + 1) + ".jpg")).queue();
                } catch (IOException e) {
                    log.warn("스레드 이미지 업로드 실패 (testId={}, idx={}): {}", detail.testId(), i, e.getMessage());
                }
            }
        });
    }

    private void recordProcess(Message message, User actor, Outcome outcome, String reason) {
        String heart = teamHeartResolver.resolve(actor.getIdLong());
        String name = actor.getEffectiveName();
        String logLine = AdminBotMessageFormatter.processLog(heart, name, outcome.from(), outcome.to(), reason);

        // 메시지당 스레드는 1개만 가능함. 스레드가 이미 있으면 그 스레드에 기록을 남기고, 없으면 새로 만듦
        ThreadChannel existing = message.getStartedThread();
        if (existing != null && !existing.isArchived()) {
            existing.sendMessage(logLine).queue();
            return;
        }
        message.createThreadChannel(THREAD_RECORD).queue(
                thread -> thread.sendMessage(logLine).queue(),
                error -> log.warn("처리 기록 스레드 생성 실패 (testId={}): {}", outcome.detail().testId(), error.getMessage()));
    }

    // 헬퍼
    boolean isAllowedChannel(String channelId) {
        String allowed = properties.bot().adminCommandChannelId();
        return allowed == null || allowed.isBlank() || allowed.equals(channelId);
    }

    private TestStatus resolveStatus(net.dv8tion.jda.api.interactions.commands.OptionMapping option) {
        return option == null ? null : TestStatus.valueOf(option.getAsString());
    }

    private int resolveInt(net.dv8tion.jda.api.interactions.commands.OptionMapping option, int defaultValue) {
        return option == null ? defaultValue : option.getAsInt();
    }

    private String value(ModalInteractionEvent event, String id) {
        var mapping = event.getValue(id);
        return mapping == null ? null : mapping.getAsString();
    }

    private void replyError(IReplyCallback event, BaseException e) {
        event.reply("오류: " + e.getMessage()).setEphemeral(true).queue();
    }
}
