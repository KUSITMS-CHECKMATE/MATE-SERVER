package server.MATE.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.auditing.DateTimeProvider;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class JpaAuditingConfigTest {

    @Test
    void auditingDateTimeProvider_truncatesToMicros_soDbRoundTripMatches() {
        // H2(MODE=PostgreSQL) 및 실제 PostgreSQL의 timestamp 컬럼은 마이크로초(6자리)까지만 저장한다.
        // 나노초 성분이 남아있으면 DB에 저장 후 재조회한 값과 flush 직후의 in-memory 엔티티 값이 달라진다.
        Instant nanoPreciseInstant = Instant.parse("2026-07-22T23:53:05.976109123Z");
        Clock fixedClock = Clock.fixed(nanoPreciseInstant, ZoneId.of("UTC"));

        DateTimeProvider provider = new JpaAuditingConfig().auditingDateTimeProvider(fixedClock);

        LocalDateTime provided = (LocalDateTime) provider.getNow().orElseThrow();

        assertThat(provided.getNano() % 1000).isZero();
    }
}
