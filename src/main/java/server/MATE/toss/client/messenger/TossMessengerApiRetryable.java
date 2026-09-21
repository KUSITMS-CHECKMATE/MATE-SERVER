package server.MATE.toss.client.messenger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

// 비즈니스 오류(200 OK+FAIL)는 재시도해도 똑같이 실패하므로, 네트워크 실패이거나 토스 서버 5xx 응답일 때만 재시도한다.
@Retryable(
        maxAttempts = 3,
        backoff = @Backoff(delay = 500, multiplier = 2),
        exceptionExpression = "#root.cause != null or #root.statusCode.is5xxServerError()"
)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TossMessengerApiRetryable {
}
