package server.MATE.domain.test.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import server.MATE.domain.test.dto.response.AdminTestDetailResponse;
import server.MATE.domain.test.dto.response.AdminTestListResponse;
import server.MATE.domain.test.dto.response.AdminTestStatusResponse;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.storage.service.FileStorageService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminTestServiceTest {

    @Mock
    private TestRepository testRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private TestCloseScheduler testCloseScheduler;

    private AdminTestService adminTestService;

    private final Long TEST_ID = 10L;

    @BeforeEach
    void setUp() {
        adminTestService = new AdminTestService(testRepository, fileStorageService, testCloseScheduler);
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
    void WAITING_테스트를_승인한다() {
        server.MATE.domain.test.entity.Test test = buildTest(TestStatus.WAITING);
        given(testRepository.findActiveById(TEST_ID)).willReturn(Optional.of(test));

        TransactionSynchronizationManager.initSynchronization();
        try {
            AdminTestStatusResponse response = adminTestService.approve(TEST_ID);

            assertThat(response.testStatus()).isEqualTo(TestStatus.IN_PROGRESS);
            TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());
            verify(testCloseScheduler).schedule(TEST_ID, test.getClosedAt());
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
}
