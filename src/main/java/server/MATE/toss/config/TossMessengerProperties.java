package server.MATE.toss.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// 앱인토스 콘솔에서 승인된 기능성 메시지 캠페인의 발송 코드. 콘솔에서 코드가 바뀌면 설정만 수정한다.
@ConfigurationProperties(prefix = "toss.messenger")
public record TossMessengerProperties(
        Templates templates
) {

    public TossMessengerProperties {
        if (templates == null) {
            templates = new Templates(null);
        }
    }

    public record Templates(
            String testApproved
    ) {
    }
}
