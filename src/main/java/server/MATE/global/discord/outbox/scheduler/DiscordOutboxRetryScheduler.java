package server.MATE.global.discord.outbox.scheduler;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import server.MATE.global.discord.outbox.entity.DiscordOutbox;
import server.MATE.global.discord.outbox.entity.DiscordOutboxStatus;
import server.MATE.global.discord.outbox.repository.DiscordOutboxRepository;
import server.MATE.global.discord.outbox.service.DiscordOutboxService;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiscordOutboxRetryScheduler {

    private final DiscordOutboxRepository outboxRepository;
    private final DiscordOutboxService outboxService;
    private final Clock clock;

    @Scheduled(fixedDelay = 60_000)
    // 최악 50건 × 5초 타임아웃을 넘는 잠금 유지로 pod 간 중복 발송 방지
    @SchedulerLock(name = "discordOutboxRetryScheduler", lockAtMostFor = "PT5M")
    public void retryPending() {
        List<DiscordOutbox> due = outboxRepository.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                DiscordOutboxStatus.PENDING, LocalDateTime.now(clock));
        if (due.isEmpty()) {
            return;
        }
        log.info("[DISCORD] outbox 재시도 대상 {}건", due.size());

        for (DiscordOutbox outbox : due) {
            try {
                outboxService.process(outbox.getId());
            } catch (Exception e) {
                log.warn("[DISCORD] outbox 재시도 처리 중 오류. outboxId={}", outbox.getId(), e);
            }
        }
    }
}
