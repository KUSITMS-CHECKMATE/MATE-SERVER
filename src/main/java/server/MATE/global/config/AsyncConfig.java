package server.MATE.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@EnableAsync
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    // 종료 시 풀별 남은 작업 대기 한도(빈 파기 단계 기준, 쿠버네티스 30초 유예 내 완료 보장 아님)
    private static final int AWAIT_TERMINATION_SECONDS = 25;

    // 알림·리워드 지급 등 짧은 비동기 작업용
    @Bean
    public ThreadPoolTaskExecutor asyncTaskExecutor() {
        return createExecutor(3, 500, "async-");
    }

    // 리포트 집계 전용(AI 분석 대기로 수 분 소요, 알림·리워드 지연 방지)
    @Bean
    public ThreadPoolTaskExecutor reportAggregateExecutor() {
        return createExecutor(2, 100, "report-aggregate-");
    }

    // @Async 기본 실행기 명시, 실행기 빈 증가 시 상한 없는 기본 실행기 폴백 방지
    @Override
    public Executor getAsyncExecutor() {
        return asyncTaskExecutor();
    }

    // 대기 줄 초과 시 호출 스레드 직접 실행(작업 유실 방지), 종료 시 남은 작업 처리
    private ThreadPoolTaskExecutor createExecutor(int poolSize, int queueCapacity, String threadNamePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(AWAIT_TERMINATION_SECONDS);
        return executor;
    }
}
