package server.MATE.toss.gateway;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.MATE.toss.client.messenger.TossMessengerApiClient;
import server.MATE.toss.dto.response.TossBulkSendMessageResponse;

@ExtendWith(MockitoExtension.class)
class TossHttpMessengerGatewayTest {

    @Mock
    private TossMessengerApiClient tossMessengerApiClient;

    private TossHttpMessengerGateway gateway;

    @BeforeEach
    void setUp() {
        gateway = new TossHttpMessengerGateway(tossMessengerApiClient);
    }

    private static TossBulkSendMessageResponse fullyDelivered(int count) {
        return new TossBulkSendMessageResponse(count, count, 0, 0, 0, 0);
    }

    @Test
    void 대상이_2500건을_넘으면_배치로_나눠_발송한다() {
        List<Long> tossUserKeys = LongStream.rangeClosed(1, 3000).boxed().toList();
        Map<String, Object> context = Map.of("title", "제목");
        given(tossMessengerApiClient.sendBulk("mate-test-approved", tossUserKeys.subList(0, 2500), context))
                .willReturn(fullyDelivered(2500));
        given(tossMessengerApiClient.sendBulk("mate-test-approved", tossUserKeys.subList(2500, 3000), context))
                .willReturn(fullyDelivered(500));

        gateway.sendBulk("mate-test-approved", tossUserKeys, context);

        verify(tossMessengerApiClient).sendBulk("mate-test-approved", tossUserKeys.subList(0, 2500), context);
        verify(tossMessengerApiClient).sendBulk("mate-test-approved", tossUserKeys.subList(2500, 3000), context);
    }

    @Test
    void 한_배치가_실패해도_나머지_배치는_계속_발송한다() {
        List<Long> tossUserKeys = LongStream.rangeClosed(1, 5000).boxed().toList();
        Map<String, Object> context = Map.of("title", "제목");
        willThrow(new RuntimeException("toss api error"))
                .given(tossMessengerApiClient).sendBulk("mate-test-approved", tossUserKeys.subList(0, 2500), context);
        given(tossMessengerApiClient.sendBulk("mate-test-approved", tossUserKeys.subList(2500, 5000), context))
                .willReturn(fullyDelivered(2500));

        gateway.sendBulk("mate-test-approved", tossUserKeys, context);

        verify(tossMessengerApiClient).sendBulk("mate-test-approved", tossUserKeys.subList(2500, 5000), context);
    }
}
