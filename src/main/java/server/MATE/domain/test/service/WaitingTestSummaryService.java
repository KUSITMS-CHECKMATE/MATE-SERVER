package server.MATE.domain.test.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import server.MATE.domain.test.dto.response.AdminTestListItemResponse;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.discord.channel.TestAlertChannel;

@Slf4j
@Service
@RequiredArgsConstructor
public class WaitingTestSummaryService {

    private static final int THRESHOLD_HOURS = 4;

    private final TestRepository testRepository;
    private final TestAlertChannel testAlertChannel;
    private final Clock clock;

    // 개별 새 테스트 알림이 끝내 실패했거나 알림 후에도 검수되지 않은 테스트를 잡는 안전망
    @Transactional(readOnly = true)
    public void notifyLongWaitingTests() {
        LocalDateTime threshold = LocalDateTime.now(clock).minusHours(THRESHOLD_HOURS);
        List<AdminTestListItemResponse> items = testRepository
                .findTop5ByTestStatusAndDeletedAtIsNullAndCreatedAtLessThanEqualOrderByCreatedAtAsc(TestStatus.WAITING, threshold)
                .stream()
                .map(AdminTestListItemResponse::from)
                .toList();
        if (items.isEmpty()) {
            return;
        }

        long totalCount = testRepository.countByTestStatusAndDeletedAtIsNullAndCreatedAtLessThanEqual(TestStatus.WAITING, threshold);
        log.info("[DISCORD] 검토 대기 요약 알림 대상 {}건", totalCount);
        testAlertChannel.notifyWaitingSummary(items, totalCount, THRESHOLD_HOURS);
    }
}
