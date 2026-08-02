package server.MATE.global.discord.bot.message;

import java.util.Set;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import server.MATE.global.discord.config.DiscordProperties;

@Component
@RequiredArgsConstructor
public class TeamHeartResolver {

    // 💜 백엔드 · 💚 프론트 · 💛 기획 · 🧡 디자인 · 🩷 그 외
    static final String BACKEND = "💜";
    static final String FRONTEND = "💚";
    static final String PLANNING = "💛";
    static final String DESIGN = "🧡";
    static final String ETC = "🩷";

    private final DiscordProperties properties;

    public String resolve(long userId) {
        DiscordProperties.Members m = properties.members();
        if (contains(m.backend(), userId)) {
            return BACKEND;
        }
        if (contains(m.frontend(), userId)) {
            return FRONTEND;
        }
        if (contains(m.planning(), userId)) {
            return PLANNING;
        }
        if (contains(m.design(), userId)) {
            return DESIGN;
        }
        return ETC;
    }

    private boolean contains(java.util.List<Long> ids, long userId) {
        return ids != null && Set.copyOf(ids).contains(userId);
    }
}
