package server.MATE.global.discord.bot.message;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import server.MATE.global.discord.config.DiscordProperties;

class TeamHeartResolverTest {

    private TeamHeartResolver resolver(DiscordProperties.Members members) {
        DiscordProperties props = new DiscordProperties("", "", null, members);
        return new TeamHeartResolver(props);
    }

    @Test
    @DisplayName("파트별로 하트 색을 판정하고, 미등록 유저는 그 외 하트를 반환한다")
    void resolve() {
        DiscordProperties.Members members = new DiscordProperties.Members(
                List.of(701L), List.of(423L), List.of(1414L), List.of(873L));
        TeamHeartResolver resolver = resolver(members);

        assertThat(resolver.resolve(701L)).isEqualTo(TeamHeartResolver.BACKEND);
        assertThat(resolver.resolve(423L)).isEqualTo(TeamHeartResolver.FRONTEND);
        assertThat(resolver.resolve(1414L)).isEqualTo(TeamHeartResolver.PLANNING);
        assertThat(resolver.resolve(873L)).isEqualTo(TeamHeartResolver.DESIGN);
        assertThat(resolver.resolve(999L)).isEqualTo(TeamHeartResolver.ETC);
    }

    @Test
    @DisplayName("멤버 설정이 비어 있으면 모두 그 외 하트")
    void resolve_emptyMembers() {
        TeamHeartResolver resolver = resolver(null);
        assertThat(resolver.resolve(701L)).isEqualTo(TeamHeartResolver.ETC);
    }
}
