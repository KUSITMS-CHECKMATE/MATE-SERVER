package server.MATE.global.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncConfigTest {

    private final AsyncConfig asyncConfig = new AsyncConfig();

    @Test
    @DisplayName("일반 비동기 풀: 일꾼 3, 대기 500, 넘치면 호출 스레드 실행, 종료 시 25초 대기")
    void asyncTaskExecutor_settings() {
        ThreadPoolTaskExecutor executor = asyncConfig.asyncTaskExecutor();
        executor.initialize();
        try {
            assertPool(executor, 3, 500, "async-");
        } finally {
            executor.shutdown();
        }
    }

    static void assertPool(ThreadPoolTaskExecutor executor, int poolSize, int queueCapacity, String prefix) {
        assertThat(executor.getCorePoolSize()).isEqualTo(poolSize);
        assertThat(executor.getMaxPoolSize()).isEqualTo(poolSize);
        assertThat(executor.getQueueCapacity()).isEqualTo(queueCapacity);
        assertThat(executor.getThreadNamePrefix()).isEqualTo(prefix);
        assertThat(executor.getThreadPoolExecutor().getRejectedExecutionHandler())
                .isInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class);
        assertThat(ReflectionTestUtils.getField(executor, "waitForTasksToCompleteOnShutdown")).isEqualTo(true);
        assertThat(ReflectionTestUtils.getField(executor, "awaitTerminationMillis")).isEqualTo(25_000L);
    }
}
