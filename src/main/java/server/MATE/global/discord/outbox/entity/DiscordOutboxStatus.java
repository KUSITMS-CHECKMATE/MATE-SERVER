package server.MATE.global.discord.outbox.entity;

public enum DiscordOutboxStatus {
    PENDING,
    SENT,
    SKIPPED,
    FAILED
}
