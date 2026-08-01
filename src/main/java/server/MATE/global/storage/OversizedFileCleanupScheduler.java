package server.MATE.global.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OversizedFileCleanupScheduler {

    private final OversizedFileCleanupService oversizedFileCleanupService;

    @Scheduled(fixedDelay = 1_800_000)
    @SchedulerLock(name = "oversizedFileCleanupScheduler", lockAtMostFor = "PT10M")
    public void cleanupOversizedFiles() {
        log.info("업로드 용량 초과 파일 정리 스케줄러 실행");
        try {
            oversizedFileCleanupService.cleanupOversizedFiles();
        } catch (Exception e) {
            log.error("업로드 용량 초과 파일 정리 실패", e);
        }
    }
}
