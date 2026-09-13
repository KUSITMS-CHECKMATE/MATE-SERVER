package server.MATE.toss.client.messenger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.MATE.toss.client.http.TossHttpClient;
import server.MATE.toss.dto.response.TossBulkSendMessageResponse;

@ExtendWith(MockitoExtension.class)
class TossMessengerApiClientTest {

    private static final String SEND_BULK_MESSAGE_PATH = "/api-partner/v1/apps-in-toss/messenger/send-bulk-message";

    @Mock
    private TossHttpClient tossHttpClient;

    private TossMessengerApiClient apiClient;

    @BeforeEach
    void setUp() {
        apiClient = new TossMessengerApiClient(tossHttpClient);
    }

    @Test
    void sendBulkBuildsContextListAndReturnsResponse() {
        TossBulkSendMessageResponse expected = new TossBulkSendMessageResponse(2, 2, 0, 0, 0, 0);
        given(tossHttpClient.post(eq(SEND_BULK_MESSAGE_PATH), org.mockito.ArgumentMatchers.any(), eq(TossBulkSendMessageResponse.class)))
                .willReturn(expected);

        TossBulkSendMessageResponse response = apiClient.sendBulk(
                "mate-test-approved", List.of(1001L, 1002L), Map.of("title", "제목")
        );

        assertThat(response).isEqualTo(expected);

        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
        verify(tossHttpClient).post(eq(SEND_BULK_MESSAGE_PATH), bodyCaptor.capture(), eq(TossBulkSendMessageResponse.class));
        Object capturedBody = bodyCaptor.getValue();
        assertThat(getFieldValue(capturedBody, "templateSetCode")).isEqualTo("mate-test-approved");

        @SuppressWarnings("unchecked")
        List<Object> contextList = (List<Object>) getFieldValue(capturedBody, "contextList");
        assertThat(contextList).hasSize(2);
        assertThat(getFieldValue(contextList.get(0), "userKey")).isEqualTo(1001L);
        assertThat(getFieldValue(contextList.get(0), "context")).isEqualTo(Map.of("title", "제목"));
        assertThat(getFieldValue(contextList.get(1), "userKey")).isEqualTo(1002L);
    }

    private Object getFieldValue(Object obj, String fieldName) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError("Could not access field: " + fieldName, e);
        }
    }
}
