package server.MATE.global.discord.bot.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import server.MATE.global.discord.bot.command.AdminTestCommands;
import server.MATE.global.discord.bot.listener.AdminBotListener;
import server.MATE.global.discord.config.DiscordProperties;

@Configuration
public class JdaConfig {

    @Bean
    @ConditionalOnProperty(prefix = "discord.bot", name = "enabled", havingValue = "true")
    public JDA jda(DiscordProperties properties, AdminBotListener adminBotListener) {
        JDA jda = JDABuilder.createLight(properties.bot().token())
                .addEventListeners(adminBotListener)
                .build();
        jda.updateCommands().addCommands(AdminTestCommands.definitions()).queue();
        return jda;
    }
}
