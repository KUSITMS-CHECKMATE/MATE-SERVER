package server.MATE;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

class MateApplicationTimeZoneTest {

    @Test
    @DisplayName("기동 시 JVM 기본 시간대를 KST로 고정")
    void applyDefaultTimeZone_setsKst() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

            MateApplication.applyDefaultTimeZone();

            assertThat(TimeZone.getDefault().getID()).isEqualTo("Asia/Seoul");
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    @DisplayName("테스트 JVM도 운영과 같은 KST 기본 시간대")
    void testJvm_defaultIsKst() {
        assertThat(TimeZone.getDefault().getID()).isEqualTo("Asia/Seoul");
    }
}
