package server.MATE.global.discord.message;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DiscordMessageRepository extends JpaRepository<DiscordMessage, Long> {

    Optional<DiscordMessage> findByTypeAndTargetId(DiscordMessageType type, Long targetId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from DiscordMessage m where m.type = :type and m.targetId = :targetId")
    int deleteByTypeAndTargetId(@Param("type") DiscordMessageType type, @Param("targetId") Long targetId);
}
