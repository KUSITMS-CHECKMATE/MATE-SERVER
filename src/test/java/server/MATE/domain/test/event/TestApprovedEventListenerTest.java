package server.MATE.domain.test.event;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import server.MATE.domain.users.entity.TossAccount;
import server.MATE.domain.users.repository.TossAccountRepository;
import server.MATE.toss.config.TossMessengerProperties;
import server.MATE.toss.gateway.TossMessengerGateway;

@ExtendWith(MockitoExtension.class)
class TestApprovedEventListenerTest {

    private static final String TEMPLATE_CODE = "mate-mate-test-approved";
    private static final Long MAKER_ID = 10L;
    private static final Long MAKER_TOSS_USER_KEY = 9999L;

    @Mock
    private TossAccountRepository tossAccountRepository;

    @Mock
    private TossMessengerGateway tossMessengerGateway;

    private TestApprovedEventListener listener;

    @BeforeEach
    void setUp() {
        listener = newListener(TEMPLATE_CODE);
    }

    private TestApprovedEventListener newListener(String templateCode) {
        return new TestApprovedEventListener(
                tossAccountRepository,
                tossMessengerGateway,
                new TossMessengerProperties(new TossMessengerProperties.Templates(templateCode))
        );
    }

    private TossAccount makerAccount() {
        return TossAccount.builder()
                .tossUserKey(MAKER_TOSS_USER_KEY)
                .isLinked(true)
                .build();
    }

    @Test
    @DisplayName("설정된 발송 코드로 연동된 유저에게 승인 알림을 발송한다")
    void handleTestApproved_sendsBulkMessageWithConfiguredTemplate() {
        given(tossAccountRepository.findByUserId(MAKER_ID)).willReturn(Optional.empty());
        given(tossAccountRepository.findTossUserKeysByIsLinkedTrue()).willReturn(List.of(1001L));

        listener.handleTestApproved(new TestApprovedEvent(1L, MAKER_ID, "제목"));

        verify(tossMessengerGateway).sendBulk(TEMPLATE_CODE, List.of(1001L), Map.of("title", "제목"));
    }

    @Test
    @DisplayName("테스트를 등록한 메이커 본인은 발송 대상에서 제외한다")
    void handleTestApproved_excludesMaker() {
        TossAccount maker = makerAccount();
        given(tossAccountRepository.findByUserId(MAKER_ID)).willReturn(Optional.of(maker));
        given(tossAccountRepository.findTossUserKeysByIsLinkedTrue())
                .willReturn(List.of(1001L, MAKER_TOSS_USER_KEY, 1002L));

        listener.handleTestApproved(new TestApprovedEvent(1L, MAKER_ID, "제목"));

        verify(tossMessengerGateway).sendBulk(TEMPLATE_CODE, List.of(1001L, 1002L), Map.of("title", "제목"));
    }

    @Test
    @DisplayName("메이커 외에 연동된 유저가 없으면 발송하지 않는다")
    void handleTestApproved_skipsWhenOnlyMakerIsLinked() {
        TossAccount maker = makerAccount();
        given(tossAccountRepository.findByUserId(MAKER_ID)).willReturn(Optional.of(maker));
        given(tossAccountRepository.findTossUserKeysByIsLinkedTrue()).willReturn(List.of(MAKER_TOSS_USER_KEY));

        listener.handleTestApproved(new TestApprovedEvent(1L, MAKER_ID, "제목"));

        verifyNoInteractions(tossMessengerGateway);
    }

    @Test
    @DisplayName("연동된 유저가 없으면 발송하지 않는다")
    void handleTestApproved_skipsWhenNoLinkedUsers() {
        given(tossAccountRepository.findByUserId(MAKER_ID)).willReturn(Optional.empty());
        given(tossAccountRepository.findTossUserKeysByIsLinkedTrue()).willReturn(List.of());

        listener.handleTestApproved(new TestApprovedEvent(1L, MAKER_ID, "제목"));

        verifyNoInteractions(tossMessengerGateway);
    }

    @Test
    @DisplayName("발송 코드가 설정되지 않았으면 발송하지 않는다")
    void handleTestApproved_skipsWhenTemplateCodeMissing() {
        TestApprovedEventListener listenerWithoutCode = newListener(null);

        listenerWithoutCode.handleTestApproved(new TestApprovedEvent(1L, MAKER_ID, "제목"));

        verifyNoInteractions(tossAccountRepository, tossMessengerGateway);
    }
}
