package server.MATE.global.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class ClockConfigTest {

    @Test
    @DisplayName("Clock 빈은 JVM 기본 시간대와 무관하게 KST 기준")
    void clock_isKst() {
        assertThat(new ClockConfig().clock().getZone()).isEqualTo(ZoneId.of("Asia/Seoul"));
    }
}
