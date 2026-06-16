package server.MATE.toss.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "toss.api")
public record TossProperties(
        boolean enabled,
        String baseUrl,
        String paymentBaseUrl,
        Timeout timeout,
        Ssl ssl
) {

    public TossProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://apps-in-toss-api.toss.im";
        }
        if (paymentBaseUrl == null || paymentBaseUrl.isBlank()) {
            paymentBaseUrl = "https://pay-apps-in-toss-api.toss.im";
        }
        if (timeout == null) {
            timeout = new Timeout(null, null);
        }
        if (ssl == null) {
            ssl = new Ssl(false, null);
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
}
