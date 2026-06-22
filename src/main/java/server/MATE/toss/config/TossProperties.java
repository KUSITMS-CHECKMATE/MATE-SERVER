package server.MATE.toss.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "toss.api")
public record TossProperties(
        boolean enabled,
        String baseUrl,
        Timeout timeout,
        Ssl ssl,
        Iap iap
) {

    public TossProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://apps-in-toss-api.toss.im";
        }
        if (timeout == null) {
            timeout = new Timeout(null, null);
        }
        if (ssl == null) {
            ssl = new Ssl(false, null);
        }
        if (iap == null) {
            iap = new Iap(List.of());
        }
    }

    public record Timeout(
            Duration connect,
            Duration read
    ) {

        public Timeout {
            if (connect == null) {
                connect = Duration.ofSeconds(5);
            }
            if (read == null) {
                read = Duration.ofSeconds(5);
            }
        }
    }

    public record Ssl(
            boolean enabled,
            String bundle
    ) {

        public Ssl {
            if (bundle == null || bundle.isBlank()) {
                bundle = "toss";
            }
        }
    }

    public record Iap(List<String> allowedSkus) {
        public Iap {
            if (allowedSkus == null) {
                allowedSkus = List.of();
            }
        }
    }
}
