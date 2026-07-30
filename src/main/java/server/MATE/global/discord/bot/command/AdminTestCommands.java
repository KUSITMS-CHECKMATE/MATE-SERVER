package server.MATE.global.discord.bot.command;

import java.util.List;

import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public final class AdminTestCommands {

    public static final String ROOT = "tests";
    public static final String SUB_LIST = "list";
    public static final String SUB_DETAIL = "detail";
    public static final String SUB_APPROVE = "approve";
    public static final String SUB_REJECT = "reject";

    public static final String OPTION_STATUS = "status";
    public static final String OPTION_PAGE = "page";
    public static final String OPTION_SIZE = "size";
    public static final String OPTION_TEST_ID = "test_id";
    public static final String OPTION_REASON = "reason";

    private AdminTestCommands() {
    }

    public static List<CommandData> definitions() {
        return List.of(
                Commands.slash(ROOT, "관리자 테스트 관리")
                        .addSubcommands(
                                new SubcommandData(SUB_LIST, "테스트 목록 조회")
                                        .addOptions(
                                                new OptionData(OptionType.STRING, OPTION_STATUS, "테스트 상태 (기본값 WAITING)")
                                                        .addChoice("WAITING", "WAITING")
                                                        .addChoice("IN_PROGRESS", "IN_PROGRESS")
                                                        .addChoice("REJECTED", "REJECTED")
                                                        .addChoice("COMPLETED", "COMPLETED"),
                                                new OptionData(OptionType.INTEGER, OPTION_PAGE, "페이지 번호 (기본값 1)"),
                                                new OptionData(OptionType.INTEGER, OPTION_SIZE, "페이지 크기 (기본값 5, 최대 20)")
                                        ),
                                new SubcommandData(SUB_DETAIL, "테스트 상세 조회")
                                        .addOption(OptionType.INTEGER, OPTION_TEST_ID, "테스트 ID", true),
                                new SubcommandData(SUB_APPROVE, "테스트 승인")
                                        .addOption(OptionType.INTEGER, OPTION_TEST_ID, "테스트 ID", true),
                                new SubcommandData(SUB_REJECT, "테스트 반려")
                                        .addOption(OptionType.INTEGER, OPTION_TEST_ID, "테스트 ID", true)
                                        .addOption(OptionType.STRING, OPTION_REASON, "반려 사유", false)
                        )
        );
    }
}
