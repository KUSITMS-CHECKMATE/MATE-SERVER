package server.MATE.domain.report.event;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import server.MATE.domain.users.entity.TossAccount;
import server.MATE.domain.users.repository.TossAccountRepository;
import server.MATE.toss.gateway.TossMessengerGateway;

@ExtendWith(MockitoExtension.class)
class ReportCompletedEventListenerTest {

    @Mock
    private TossAccountRepository tossAccountRepository;

    @Mock
    private TossMessengerGateway tossMessengerGateway;

    @Mock
    private TossAccount tossAccount;

    @InjectMocks
    private ReportCompletedEventListener listener;

    @Test
    @DisplayName("메이커가 토스 연동돼 있으면 리포트 완료 알림을 발송한다")
    void handleReportCompleted_sendsSingleMessageToLinkedMaker() {
        given(tossAccountRepository.findByUserId(1L)).willReturn(Optional.of(tossAccount));
        given(tossAccount.isLinked()).willReturn(true);
        given(tossAccount.getTossUserKey()).willReturn(1001L);

        listener.handleReportCompleted(new ReportCompletedEvent(10L, 1L, "제목"));

        verify(tossMessengerGateway).sendSingle(
                "mate-report-completed",
                1001L,
                Map.of("testId", "10", "title", "제목")
        );
    }

    @Test
    @DisplayName("메이커가 토스 연동돼 있지 않으면 발송하지 않는다")
    void handleReportCompleted_skipsWhenNotLinked() {
        given(tossAccountRepository.findByUserId(1L)).willReturn(Optional.of(tossAccount));
        given(tossAccount.isLinked()).willReturn(false);

        listener.handleReportCompleted(new ReportCompletedEvent(10L, 1L, "제목"));

        verifyNoInteractions(tossMessengerGateway);
    }

    @Test
    @DisplayName("메이커의 토스 계정이 없으면 발송하지 않는다")
    void handleReportCompleted_skipsWhenNoTossAccount() {
        given(tossAccountRepository.findByUserId(1L)).willReturn(Optional.empty());

        listener.handleReportCompleted(new ReportCompletedEvent(10L, 1L, "제목"));

        verifyNoInteractions(tossMessengerGateway);
    }
}
