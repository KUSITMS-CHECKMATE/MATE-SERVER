package server.MATE;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import server.MATE.global.config.ClockConfig;

import java.util.TimeZone;

@EnableScheduling
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
@SpringBootApplication
public class MateApplication {

	public static void main(String[] args) {
		applyDefaultTimeZone();
		SpringApplication.run(MateApplication.class, args);
	}

	// JVM 기본 시간대 KST 고정, LocalDateTime.now()와 자동 기록 시각의 기준 통일용
	static void applyDefaultTimeZone() {
		TimeZone.setDefault(TimeZone.getTimeZone(ClockConfig.KST));
	}

}
