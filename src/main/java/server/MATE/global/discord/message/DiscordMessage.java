package server.MATE.global.discord.message;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.MATE.global.common.entity.BaseEntity;

// 봇이 보낸 Discord 메시지 위치 기록용. 전송 후 수정·스레드 이어쓰기에 사용함
@Getter
@Entity
@Table(
        name = "discord_message",
        uniqueConstraints = @UniqueConstraint(name = "uk_discord_message_type_target", columnNames = {"type", "target_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiscordMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DiscordMessageType type;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "channel_id", nullable = false, length = 32)
    private String channelId;

    // 메시지에서 만든 스레드 ID와 같음
    @Column(name = "message_id", nullable = false, length = 32)
    private String messageId;

    private DiscordMessage(DiscordMessageType type, Long targetId, String channelId, String messageId) {
        this.type = type;
        this.targetId = targetId;
        this.channelId = channelId;
        this.messageId = messageId;
    }

    public static DiscordMessage create(DiscordMessageType type, Long targetId, String channelId, String messageId) {
        return new DiscordMessage(type, targetId, channelId, messageId);
    }
}
