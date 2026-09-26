package server.MATE.domain.test.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import server.MATE.domain.payment.entity.PayStatus;
import server.MATE.domain.payment.entity.Payment;
import server.MATE.domain.payment.repository.PaymentRepository;
import server.MATE.domain.test.dto.response.AdminTestDetailResponse;
import server.MATE.domain.test.dto.response.AdminTestListResponse;
import server.MATE.domain.test.dto.response.AdminTestProgressResponse;
import server.MATE.domain.test.dto.response.AdminTestStatusResponse;
import server.MATE.domain.test.entity.ReportStatus;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.event.TestApprovedEvent;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.storage.service.FileStorageService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminTestServiceTest {

    @Mock
    private TestRepository testRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private TestCloseScheduler testCloseScheduler;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PaymentRepository paymentRepository;

    // KST 2026-09-27 10:00:00
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-27T01:00:00Z"), ZoneId.of("UTC"));
    private static final LocalDateTime NEW_CLOSED_AT = LocalDateTime.of(2026, 10, 20, 23, 59, 59);

    private AdminTestService adminTestService;

    private final Long TEST_ID = 10L;

    @BeforeEach
    void setUp() {
        adminTestService = new AdminTestService(
                testRepository, fileStorageService, testCloseScheduler, eventPublisher, paymentRepository, CLOCK);
        lenient().when(fileStorageService.generateDownloadUrl(anyString())).thenReturn("https://example.com/url");
    }

    private server.MATE.domain.test.entity.Test buildTest(TestStatus status) {
        server.MATE.domain.test.entity.Test test = server.MATE.domain.test.entity.Test.builder()
                .makerId(1L)
                .title("테스트")
                .description("설명")
                .serviceName("서비스")
                .serviceDescription("서비스 설명")
                .imageKeys(List.of("key-1"))
                .goalPpl(10)
                .reward(300)
                .testStatus(status)
                .closedAt(LocalDateTime.now().plusDays(7))
                .build();
        ReflectionTestUtils.setField(test, "id", TEST_ID);
        return test;
    }

    @Test
    void 목록을_기본값으로_조회한다() {
        server.MATE.domain.test.entity.Test test = buildTest(TestStatus.WAITING);
        given(testRepository.findByStatusForAdmin(TestStatus.WAITING, 0, 20)).willReturn(List.of(test));
        given(testRepository.countByStatusForAdmin(TestStatus.WAITING)).willReturn(1L);

        AdminTestListResponse response = adminTestService.listTests(null, 1, 20);

        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalCount()).isEqualTo(1L);
        assertThat(response.tests()).hasSize(1);
    }

    @Test
    void 페이지_번호로_offset을_계산한다() {
        given(testRepository.findByStatusForAdmin(TestStatus.WAITING, 40, 20)).willReturn(List.of());
        given(testRepository.countByStatusForAdmin(TestStatus.WAITING)).willReturn(0L);

        adminTestService.listTests(TestStatus.WAITING, 3, 20);

        verify(testRepository).findByStatusForAdmin(TestStatus.WAITING, 40, 20);
    }

    @Test
    void 상세_조회_시_이미지_URL을_변환한다() {
        server.MATE.domain.test.entity.Test test = buildTest(TestStatus.REJECTED);
        given(testRepository.findWithCategoriesByIdForAdmin(TEST_ID)).willReturn(Optional.of(test));

        AdminTestDetailResponse response = adminTestService.getTest(TEST_ID);

        assertThat(response.testId()).isEqualTo(TEST_ID);
        assertThat(response.imageUrls()).containsExactly("https://example.com/url");
    }

    @Test
    void 존재하지_않는_테스트_상세_조회시_예외가_발생한다() {
        given(testRepository.findWithCategoriesByIdForAdmin(TEST_ID)).willReturn(Optional.empty());

        BaseException exception = assertThrows(BaseException.class,
                () -> adminTestService.getTest(TEST_ID));

        assertThat(exception.getErrorCode()).isEqualTo(BaseErrorCode.TEST_004);
    }

    @Test
    void 참여_현황_조회_시_목표_인원_참여_인원_달성률을_반환한다() {
        server.MATE.domain.test.entity.Test test = buildTest(TestStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(test, "goalPpl", 100);
        ReflectionTestUtils.setField(test, "pplCount", 29L);
        given(testRepository.findWithCategoriesByIdForAdmin(TEST_ID)).willReturn(Optional.of(test));

        AdminTestProgressResponse response = adminTestService.getProgress(TEST_ID);

        assertThat(response.testId()).isEqualTo(TEST_ID);
        assertThat(response.title()).isEqualTo("테스트");
        assertThat(response.description()).isEqualTo("설명");
        assertThat(response.testStatus()).isEqualTo(TestStatus.IN_PROGRESS);
        assertThat(response.goalPpl()).isEqualTo(100);
        assertThat(response.pplCount()).isEqualTo(29L);
        assertThat(response.achievementPercent()).isEqualTo(29);
        assertThat(response.thumbnailUrl()).isEqualTo("https://example.com/url");
    }

    @Test
    void 참여_현황_달성률은_소수점을_버린다() {
        server.MATE.domain.test.entity.Test test = buildTest(TestStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(test, "goalPpl", 1000);
        ReflectionTestUtils.setField(test, "pplCount", 996L);
        given(testRepository.findWithCategoriesByIdForAdmin(TEST_ID)).willReturn(Optional.of(test));

        AdminTestProgressResponse response = adminTestService.getProgress(TEST_ID);

        assertThat(response.achievementPercent()).isEqualTo(99);
    }

    @Test
    void 참여_현황_목표_인원이_0이면_달성률은_0이다() {
        server.MATE.domain.test.entity.Test test = buildTest(TestStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(test, "goalPpl", 0);
        given(testRepository.findWithCategoriesByIdForAdmin(TEST_ID)).willReturn(Optional.of(test));

        AdminTestProgressResponse response = adminTestService.getProgress(TEST_ID);

        assertThat(response.achievementPercent()).isZero();
    }

    @Test
    void 참여_현황_이미지가_없으면_썸네일은_null이다() {
        server.MATE.domain.test.entity.Test test = buildTest(TestStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(test, "imageKeys", new ArrayList<String>());
        given(testRepository.findWithCategoriesByIdForAdmin(TEST_ID)).willReturn(Optional.of(test));

        AdminTestProgressResponse response = adminTestService.getProgress(TEST_ID);

        assertThat(response.thumbnailUrl()).isNull();
    }

    @Test
    void 존재하지_않는_테스트_참여_현황_조회시_예외가_발생한다() {
        given(testRepository.findWithCategoriesByIdForAdmin(TEST_ID)).willReturn(Optional.empty());

        BaseException exception = assertThrows(BaseException.class,
                () -> adminTestService.getProgress(TEST_ID));

        assertThat(exception.getErrorCode()).isEqualTo(BaseErrorCode.TEST_004);
    }

    @Test
    void WAITING_테스트를_승인한다() {
        server.MATE.domain.test.entity.Test test = buildTest(TestStatus.WAITING);
        given(testRepository.findActiveById(TEST_ID)).willReturn(Optional.of(test));

        TransactionSynchronizationManager.initSynchronization();
        try {
            AdminTestStatusResponse response = adminTestService.approve(TEST_ID);

            assertThat(response.testStatus()).isEqualTo(TestStatus.IN_PROGRESS);
            TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());
            verify(testCloseScheduler).schedule(TEST_ID, test.getClosedAt());
            verify(eventPublisher).publishEvent(new TestApprovedEvent(TEST_ID, test.getTitle()));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void REJECTED_테스트를_재승인한다() {
        server.MATE.domain.test.entity.Test test = buildTest(TestStatus.REJECTED);
        given(testRepository.findActiveById(TEST_ID)).willReturn(Optional.of(test));

        TransactionSynchronizationManager.initSynchronization();
        try {
            AdminTestStatusResponse response = adminTestService.approve(TEST_ID);

            assertThat(response.testStatus()).isEqualTo(TestStatus.IN_PROGRESS);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void COMPLETED_테스트_승인_시도시_예외가_발생한다() {
        server.MATE.domain.test.entity.Test test = buildTest(TestStatus.COMPLETED);
        given(testRepository.findActiveById(TEST_ID)).willReturn(Optional.of(test));

        BaseException exception = assertThrows(BaseException.class,
                () -> adminTestService.approve(TEST_ID));

        assertThat(exception.getErrorCode()).isEqualTo(BaseErrorCode.TEST_007);
    }

    @Test
    void WAITING_테스트를_반려한다() {
        server.MATE.domain.test.entity.Test test = buildTest(TestStatus.WAITING);
        given(testRepository.findActiveById(TEST_ID)).willReturn(Optional.of(test));

        AdminTestStatusResponse response = adminTestService.reject(TEST_ID, "사유");

        assertThat(response.testStatus()).isEqualTo(TestStatus.REJECTED);
        assertThat(test.getRejectionReason()).isEqualTo("사유");
    }

    @Test
    void IN_PROGRESS_테스트를_후반_반려한다() {
        server.MATE.domain.test.entity.Test test = buildTest(TestStatus.IN_PROGRESS);
        given(testRepository.findActiveById(TEST_ID)).willReturn(Optional.of(test));

        AdminTestStatusResponse response = adminTestService.reject(TEST_ID, null);

        assertThat(response.testStatus()).isEqualTo(TestStatus.REJECTED);
        assertThat(test.getRejectionReason()).isNull();
    }

    @Test
    void COMPLETED_테스트_반려_시도시_예외가_발생한다() {
        server.MATE.domain.test.entity.Test test = buildTest(TestStatus.COMPLETED);
        given(testRepository.findActiveById(TEST_ID)).willReturn(Optional.of(test));

        BaseException exception = assertThrows(BaseException.class,
                () -> adminTestService.reject(TEST_ID, null));

        assertThat(exception.getErrorCode()).isEqualTo(BaseErrorCode.TEST_007);
    }

    @Test
    void 완료_테스트를_재개하면_진행_중으로_돌아가고_커밋_후_마감을_예약한다() {
        server.MATE.domain.test.entity.Test test = completedTest(5L);
        test.markClosedByMaker();
        test.waiveRefund();
        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.of(test));
        given(paymentRepository.findByTestId(TEST_ID)).willReturn(Optional.empty());

        TransactionSynchronizationManager.initSynchronization();
        try {
            AdminTestStatusResponse response = adminTestService.reopen(TEST_ID, NEW_CLOSED_AT);

            assertThat(response.testStatus()).isEqualTo(TestStatus.IN_PROGRESS);
            assertThat(test.getClosedAt()).isEqualTo(NEW_CLOSED_AT);
            assertThat(test.isClosedByMaker()).isFalse();
            assertThat(test.isRefundWaived()).isTrue();
            assertThat(test.getReopenedAt()).isEqualTo(LocalDateTime.of(2026, 9, 27, 1, 0));
            TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());
            verify(testCloseScheduler).schedule(TEST_ID, NEW_CLOSED_AT);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void 존재하지_않는_테스트_재개시_TEST_004() {
        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.empty());

        BaseException e = assertThrows(BaseException.class, () -> adminTestService.reopen(TEST_ID, NEW_CLOSED_AT));

        assertThat(e.getErrorCode()).isEqualTo(BaseErrorCode.TEST_004);
    }

    @Test
    void 완료가_아닌_테스트_재개시_TEST_007() {
        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.of(buildTest(TestStatus.IN_PROGRESS)));

        BaseException e = assertThrows(BaseException.class, () -> adminTestService.reopen(TEST_ID, NEW_CLOSED_AT));

        assertThat(e.getErrorCode()).isEqualTo(BaseErrorCode.TEST_007);
    }

    @Test
    void reopen_closedAtEqualToNow_throwsTest011() {
        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.of(completedTest(5L)));

        BaseException e = assertThrows(BaseException.class,
                () -> adminTestService.reopen(TEST_ID, LocalDateTime.of(2026, 9, 27, 10, 0)));

        assertThat(e.getErrorCode()).isEqualTo(BaseErrorCode.TEST_011);
    }

    @Test
    void 목표_인원을_채운_테스트_재개시_TEST_012() {
        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.of(completedTest(10L)));

        BaseException e = assertThrows(BaseException.class, () -> adminTestService.reopen(TEST_ID, NEW_CLOSED_AT));

        assertThat(e.getErrorCode()).isEqualTo(BaseErrorCode.TEST_012);
    }

    @Test
    void 리포트_집계_중인_테스트_재개시_TEST_013() {
        server.MATE.domain.test.entity.Test test = completedTest(5L);
        test.startReportAggregation();
        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.of(test));

        BaseException e = assertThrows(BaseException.class, () -> adminTestService.reopen(TEST_ID, NEW_CLOSED_AT));

        assertThat(e.getErrorCode()).isEqualTo(BaseErrorCode.TEST_013);
    }

    @ParameterizedTest
    @EnumSource(value = PayStatus.class, names = {"REFUND_PENDING", "REFUNDED"})
    void 환불이_시작된_테스트_재개시_TEST_014(PayStatus payStatus) {
        Payment payment = payment(payStatus);
        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.of(completedTest(1L)));
        given(paymentRepository.findByTestId(TEST_ID)).willReturn(Optional.of(payment));

        BaseException e = assertThrows(BaseException.class, () -> adminTestService.reopen(TEST_ID, NEW_CLOSED_AT));

        assertThat(e.getErrorCode()).isEqualTo(BaseErrorCode.TEST_014);
    }

    @Test
    void reopen_withoutPayment_succeeds() {
        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.of(completedTest(1L)));
        given(paymentRepository.findByTestId(TEST_ID)).willReturn(Optional.empty());

        TransactionSynchronizationManager.initSynchronization();
        try {
            assertThat(adminTestService.reopen(TEST_ID, NEW_CLOSED_AT).testStatus()).isEqualTo(TestStatus.IN_PROGRESS);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void reopen_refundRejectedPayment_succeeds() {
        Payment payment = payment(PayStatus.REFUND_REJECTED);
        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.of(completedTest(1L)));
        given(paymentRepository.findByTestId(TEST_ID)).willReturn(Optional.of(payment));

        TransactionSynchronizationManager.initSynchronization();
        try {
            assertThat(adminTestService.reopen(TEST_ID, NEW_CLOSED_AT).testStatus()).isEqualTo(TestStatus.IN_PROGRESS);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void 리포트_집계에_실패한_테스트도_재개할_수_있다() {
        server.MATE.domain.test.entity.Test test = completedTest(5L);
        test.startReportAggregation();
        test.failReportAggregation();
        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.of(test));
        given(paymentRepository.findByTestId(TEST_ID)).willReturn(Optional.empty());

        TransactionSynchronizationManager.initSynchronization();
        try {
            adminTestService.reopen(TEST_ID, NEW_CLOSED_AT);

            assertThat(test.getReportStatus()).isEqualTo(ReportStatus.PENDING);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private server.MATE.domain.test.entity.Test completedTest(long pplCount) {
        server.MATE.domain.test.entity.Test test = buildTest(TestStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(test, "pplCount", pplCount);
        test.complete();
        return test;
    }

    // 다른 given(...) 안에서 호출하면 UnfinishedStubbingException이 나므로 지역 변수로 먼저 생성해 사용함
    private Payment payment(PayStatus payStatus) {
        Payment payment = mock(Payment.class);
        given(payment.getPayStatus()).willReturn(payStatus);
        return payment;
    }
}
