package server.MATE.domain.report.service.excel.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import server.MATE.domain.test.entity.ReportStatus;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ReportExcelExportSupportTest {

    @Mock
    private TestRepository testRepository;

    @InjectMocks
    private ReportExcelExportSupport reportExcelExportSupport;

    @Test
    void 테스트가_종료되지_않으면_엑셀_다운로드를_허용하지_않는다() {
        server.MATE.domain.test.entity.Test test = server.MATE.domain.test.entity.Test.builder()
                .makerId(1L)
                .testStatus(TestStatus.IN_PROGRESS)
                .build();
        ReflectionTestUtils.setField(test, "reportStatus", ReportStatus.PENDING);

        given(testRepository.findActiveById(10L)).willReturn(Optional.of(test));

        assertThatThrownBy(() -> reportExcelExportSupport.requireExportReadyTest(10L, 1L))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(BaseErrorCode.TEST_006));
    }

    @Test
    void 리포트_집계가_완료되지_않으면_엑셀_다운로드를_허용하지_않는다() {
        server.MATE.domain.test.entity.Test test = server.MATE.domain.test.entity.Test.builder()
                .makerId(1L)
                .testStatus(TestStatus.COMPLETED)
                .build();
        ReflectionTestUtils.setField(test, "reportStatus", ReportStatus.IN_PROGRESS);

        given(testRepository.findActiveById(10L)).willReturn(Optional.of(test));

        assertThatThrownBy(() -> reportExcelExportSupport.requireExportReadyTest(10L, 1L))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(BaseErrorCode.REPORT_007));
    }
}
