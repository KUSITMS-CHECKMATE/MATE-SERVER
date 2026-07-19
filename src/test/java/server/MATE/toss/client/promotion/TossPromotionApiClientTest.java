package server.MATE.toss.client.promotion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import server.MATE.toss.client.http.TossHttpClient;
import server.MATE.toss.dto.response.TossPromotionExecuteResponse;
import server.MATE.toss.dto.response.TossPromotionExecutionStatus;
import server.MATE.toss.dto.response.TossPromotionKeyResponse;

@ExtendWith(MockitoExtension.class)
class TossPromotionApiClientTest {

    private static final String GET_KEY_PATH = "/api-partner/v1/apps-in-toss/promotion/execute-promotion/get-key";
    private static final String EXECUTE_PATH = "/api-partner/v1/apps-in-toss/promotion/execute-promotion";
    private static final String RESULT_PATH = "/api-partner/v1/apps-in-toss/promotion/execution-result";

    @Mock
    private TossHttpClient tossHttpClient;

    private TossPromotionApiClient apiClient;

    @BeforeEach
    void setUp() {
        apiClient = new TossPromotionApiClient(tossHttpClient);
    }

    @Test
    void issueKeySendsUserKeyHeaderAndReturnsResponse() {
        given(tossHttpClient.post(eq(GET_KEY_PATH), any(), ArgumentMatchers.<Consumer<HttpHeaders>>any(), eq(TossPromotionKeyResponse.class)))
                .willReturn(new TossPromotionKeyResponse("issued-key"));

        TossPromotionKeyResponse response = apiClient.issueKey(777L);

        assertThat(response.key()).isEqualTo("issued-key");
        ArgumentCaptor<Consumer<HttpHeaders>> headerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(tossHttpClient).post(eq(GET_KEY_PATH), any(), headerCaptor.capture(), eq(TossPromotionKeyResponse.class));
        HttpHeaders headers = new HttpHeaders();
        headerCaptor.getValue().accept(headers);
        assertThat(headers.getFirst("x-toss-user-key")).isEqualTo("777");
    }

    @Test
    void executeSendsPromotionCodeRewardKeyAndAmount() {
        given(tossHttpClient.post(eq(EXECUTE_PATH), any(), ArgumentMatchers.<Consumer<HttpHeaders>>any(), eq(TossPromotionExecuteResponse.class)))
                .willReturn(new TossPromotionExecuteResponse("reward-key"));

        TossPromotionExecuteResponse response = apiClient.execute(777L, "promo-code", "reward-key", 1000);

        assertThat(response.key()).isEqualTo("reward-key");
        verify(tossHttpClient).post(eq(EXECUTE_PATH), any(), ArgumentMatchers.<Consumer<HttpHeaders>>any(), eq(TossPromotionExecuteResponse.class));
    }

    @Test
    void getExecutionStatusReturnsRawStatus() {
        given(tossHttpClient.post(eq(RESULT_PATH), any(), ArgumentMatchers.<Consumer<HttpHeaders>>any(), eq(TossPromotionExecutionStatus.class)))
                .willReturn(TossPromotionExecutionStatus.SUCCESS);

        TossPromotionExecutionStatus status = apiClient.getExecutionStatus(777L, "promo-code", "reward-key");

        assertThat(status).isEqualTo(TossPromotionExecutionStatus.SUCCESS);
    }
}
