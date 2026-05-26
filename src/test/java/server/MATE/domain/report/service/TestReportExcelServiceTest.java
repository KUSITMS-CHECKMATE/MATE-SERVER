package server.MATE.domain.report.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.MATE.domain.question.dto.response.QuestionSummaryItem;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.report.dto.response.TestReportExcelDownload;
import server.MATE.domain.report.excel.MateReportExcelWriter;
import server.MATE.domain.report.excel.TestReportExcelData;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TestReportExcelServiceTest {

    @Mock
    private TestRepository testRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private MateReportExcelWriter mateReportExcelWriter;

    @InjectMocks
    private TestReportExcelService testReportExcelService;

    @Test
    void 메이커는_엑셀_보고서를_다운로드할_수_있다() {
        server.MATE.domain.test.entity.Test test = server.MATE.domain.test.entity.Test.builder()
                .makerId(1L)
                .title("테스트")
                .description("설명")
                .build();
        List<QuestionSummaryItem> questions = List.of(
                new QuestionSummaryItem(1L, 1L, "질문 1", QuestionType.OBJECTIVE)
        );

        given(testRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(test));
        given(questionRepository.findQuestionSummariesByTestId(10L)).willReturn(questions);
        given(mateReportExcelWriter.write(any(TestReportExcelData.class))).willReturn(new byte[]{1, 2, 3});

        TestReportExcelDownload download = testReportExcelService.export(10L, 1L);

        assertThat(download.filename()).isEqualTo("mate-report-10.xlsx");
        assertThat(download.content()).containsExactly(1, 2, 3);
        verify(mateReportExcelWriter).write(any(TestReportExcelData.class));
    }

    @Test
    void 질문이_21개면_엑셀_다운로드를_허용하지_않는다() {
        server.MATE.domain.test.entity.Test test = server.MATE.domain.test.entity.Test.builder().makerId(1L).title("테스트").build();
        List<QuestionSummaryItem> questions = IntStream.rangeClosed(1, 21)
                .mapToObj(index -> new QuestionSummaryItem(
                        (long) index,
                        (long) index,
                        "질문 " + index,
                        QuestionType.OBJECTIVE
                ))
                .toList();

        given(testRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(test));
        given(questionRepository.findQuestionSummariesByTestId(10L)).willReturn(questions);

        assertThatThrownBy(() -> testReportExcelService.export(10L, 1L))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(BaseErrorCode.REPORT_001));
    }
}
