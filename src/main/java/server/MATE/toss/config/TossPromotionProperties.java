package server.MATE.toss.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "toss.promotion")
public record TossPromotionProperties(
        Answer answer
) {

    public TossPromotionProperties {
        if (answer == null) {
            answer = new Answer(false, null);
        }
    }

    public record Answer(
            boolean enabled,
            String promotionCode
    ) {
    }
}
