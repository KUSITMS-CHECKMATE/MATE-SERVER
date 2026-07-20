package server.MATE.global.discord.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DiscordProperties.class)
public class DiscordConfig {
}
