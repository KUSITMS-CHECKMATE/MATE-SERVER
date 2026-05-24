package server.MATE.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Configuration
public class JpaAuditingConfig {

    @Bean(name = "auditingDateTimeProvider")
    public DateTimeProvider auditingDateTimeProvider(Clock clock) {
        return () -> Optional.of(LocalDateTime.now(clock));
    }
}
