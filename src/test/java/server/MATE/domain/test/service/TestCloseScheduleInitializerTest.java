package server.MATE.domain.test.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.test.util.ReflectionTestUtils;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.repository.TestRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TestCloseScheduleInitializerTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Mock
    private TestRepository testRepository;

    @Mock
    private TestCloseScheduler testCloseScheduler;

    @Mock
    private TestCloseProcessor testCloseProcessor;

    private TestCloseScheduleInitializer initializer;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-12T03:00:00Z"), KST);
        initializer = new TestCloseScheduleInitializer(
                testRepository,
                testCloseScheduler,
                testCloseProcessor,
                clock
        );
    }

    @Test
    @DisplayName("마감 시각이 남은 진행 중 테스트는 자동 종료 예약을 복원한다")
    void restoresScheduleForFutureTests() throws Exception {
        server.MATE.domain.test.entity.Test futureTest = inProgressTest(
                1L,
                LocalDateTime.of(2099, 12, 31, 23, 59, 59)
        );
        given(testRepository.findByTestStatusAndDeletedAtIsNull(TestStatus.IN_PROGRESS))
                .willReturn(List.of(futureTest));

        initializer.run(new DefaultApplicationArguments(new String[]{}));

        verify(testCloseScheduler).schedule(1L, futureTest.getClosedAt());
        verify(testCloseProcessor, never()).process(1L);
    }

    @Test
    @DisplayName("마감 시각이 지난 진행 중 테스트는 즉시 종료한다")
    void closesExpiredTestsImmediately() throws Exception {
        server.MATE.domain.test.entity.Test expiredTest = inProgressTest(
                2L,
                LocalDateTime.of(2020, 1, 1, 23, 59, 59)
        );
        given(testRepository.findByTestStatusAndDeletedAtIsNull(TestStatus.IN_PROGRESS))
                .willReturn(List.of(expiredTest));

        initializer.run(new DefaultApplicationArguments(new String[]{}));

        verify(testCloseProcessor).process(2L);
        verify(testCloseScheduler, never()).schedule(2L, expiredTest.getClosedAt());
    }

    @Test
    @DisplayName("closedAt이 없는 진행 중 테스트는 복원 대상에서 제외한다")
    void skipsTestsWithoutClosedAt() throws Exception {
        server.MATE.domain.test.entity.Test testWithoutClosedAt = inProgressTest(
                3L,
                LocalDateTime.of(2099, 12, 31, 23, 59, 59)
        );
        ReflectionTestUtils.setField(testWithoutClosedAt, "closedAt", null);
        given(testRepository.findByTestStatusAndDeletedAtIsNull(TestStatus.IN_PROGRESS))
                .willReturn(List.of(testWithoutClosedAt));

        initializer.run(new DefaultApplicationArguments(new String[]{}));

        verify(testCloseScheduler, never()).schedule(3L, null);
        verify(testCloseProcessor, never()).process(3L);
    }

    private server.MATE.domain.test.entity.Test inProgressTest(Long id, LocalDateTime closedAt) {
        server.MATE.domain.test.entity.Test test = server.MATE.domain.test.entity.Test.builder()
                .makerId(1L)
                .title("테스트")
                .description("설명")
                .serviceName("서비스")
                .serviceDescription("서비스 설명")
                .imageKeys(List.of())
                .closedAt(closedAt)
                .testStatus(TestStatus.IN_PROGRESS)
                .build();
        ReflectionTestUtils.setField(test, "id", id);
        return test;
    }
}
