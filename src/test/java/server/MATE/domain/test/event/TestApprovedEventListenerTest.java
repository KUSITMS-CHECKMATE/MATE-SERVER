package server.MATE.domain.test.event;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import server.MATE.domain.users.repository.TossAccountRepository;
import server.MATE.toss.gateway.TossMessengerGateway;

@ExtendWith(MockitoExtension.class)
class TestApprovedEventListenerTest {

    @Mock
    private TossAccountRepository tossAccountRepository;

    @Mock
    private TossMessengerGateway tossMessengerGateway;

    @InjectMocks
    private TestApprovedEventListener listener;

    @Test
    @DisplayName("연동된 유저가 있으면 승인 알림을 발송한다")
    void handleTestApproved_sendsBulkMessageToLinkedUsers() {
        given(tossAccountRepository.findTossUserKeysByIsLinkedTrue()).willReturn(List.of(1001L));

        listener.handleTestApproved(new TestApprovedEvent(1L, "제목"));

        verify(tossMessengerGateway).sendBulk("mate-test-approved", List.of(1001L), Map.of("title", "제목"));
    }

    @Test
    @DisplayName("연동된 유저가 없으면 발송하지 않는다")
    void handleTestApproved_skipsWhenNoLinkedUsers() {
        given(tossAccountRepository.findTossUserKeysByIsLinkedTrue()).willReturn(List.of());

        listener.handleTestApproved(new TestApprovedEvent(1L, "제목"));

        verifyNoInteractions(tossMessengerGateway);
    }
}
