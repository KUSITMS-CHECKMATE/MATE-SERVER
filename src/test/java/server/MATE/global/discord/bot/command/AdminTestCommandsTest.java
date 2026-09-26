package server.MATE.global.discord.bot.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

class AdminTestCommandsTest {

    @Test
    @DisplayName("/tests list 옵션은 status 하나(페이지 이동은 버튼으로만)")
    void listSubcommand_hasOnlyStatusOption() {
        SlashCommandData root = (SlashCommandData) AdminTestCommands.definitions().get(0);
        SubcommandData list = root.getSubcommands().stream()
                .filter(sub -> sub.getName().equals(AdminTestCommands.SUB_LIST))
                .findFirst()
                .orElseThrow();

        assertThat(list.getOptions())
                .extracting(OptionData::getName)
                .containsExactly(AdminTestCommands.OPTION_STATUS);
    }
}
