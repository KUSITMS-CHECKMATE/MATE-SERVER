package server.MATE.domain.report.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.entity.TreeTest;
import server.MATE.domain.question.repository.TreeTestRepository;
import server.MATE.domain.report.dto.response.TestReportExcelDownload;
import server.MATE.domain.report.excel.TreeTestReportExcelData;
import server.MATE.domain.report.excel.TreeTestReportExcelWriter;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TreeTestReportExcelServiceTest {

    @Mock
    private ReportExcelExportSupport reportExcelExportSupport;

    @Mock
    private TreeTestRepository treeTestRepository;

    @Mock
    private TreeTestReportExcelWriter treeTestReportExcelWriter;

    @InjectMocks
    private TreeTestReportExcelService treeTestReportExcelService;

    @Test
    void 메이커는_트리_테스트_통계_엑셀을_다운로드할_수_있다() {
        server.MATE.domain.test.entity.Test test = server.MATE.domain.test.entity.Test.builder()
                .makerId(1L)
                .title("테스트")
                .build();

        Question question = Question.builder()
                .testId(10L)
                .questionType(QuestionType.TREE_TEST)
                .title("고객센터를 찾아보세요.")
                .sequence(1L)
                .build();

        TreeTest customerCenter = org.mockito.Mockito.mock(TreeTest.class);
        given(customerCenter.getId()).willReturn(12L);
        given(customerCenter.getLabel()).willReturn("고객센터");

        Answer answer = Answer.builder()
                .participationId(100L)
                .questionId(20L)
                .questionType(QuestionType.TREE_TEST)
                .answer(Map.of(
                        "nodeId", 12L,
                        "path", List.of(1L, 5L, 12L)
                ))
                .build();

        Map<String, Object> reportResult = Map.of(
                "pathFrequency", List.of(
                        Map.of(
                                "path", List.of(1L, 5L, 12L),
                                "pathLabels", List.of("홈", "지원", "고객센터"),
                                "count", 1
                        )
                )
        );

        given(reportExcelExportSupport.requireExportReadyTest(10L, 1L)).willReturn(test);
        given(reportExcelExportSupport.requireQuestion(10L, 20L, QuestionType.TREE_TEST, BaseErrorCode.REPORT_008))
                .willReturn(question);
        given(reportExcelExportSupport.requireReportResult(10L, 20L)).willReturn(reportResult);
        given(treeTestRepository.findAllByQuestionIdInOrderByQuestionAndTree(List.of(20L)))
                .willReturn(List.of(customerCenter));
        given(reportExcelExportSupport.loadAnswers(20L)).willReturn(List.of(answer));
        given(treeTestReportExcelWriter.write(any(TreeTestReportExcelData.class))).willReturn(new byte[]{1, 2, 3});

        TestReportExcelDownload download = treeTestReportExcelService.export(10L, 20L, 1L);

        assertThat(download.filename()).isEqualTo("mate-tree-test-report-10-q01.xlsx");
        assertThat(download.content()).containsExactly(1, 2, 3);
        verify(treeTestReportExcelWriter).write(any(TreeTestReportExcelData.class));
    }

    @Test
    void 트리_테스트가_아니면_다운로드를_허용하지_않는다() {
        given(reportExcelExportSupport.requireExportReadyTest(10L, 1L))
                .willReturn(server.MATE.domain.test.entity.Test.builder().makerId(1L).build());
        given(reportExcelExportSupport.requireQuestion(10L, 20L, QuestionType.TREE_TEST, BaseErrorCode.REPORT_008))
                .willThrow(new BaseException(BaseErrorCode.REPORT_008));

        assertThatThrownBy(() -> treeTestReportExcelService.export(10L, 20L, 1L))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(BaseErrorCode.REPORT_008));
    }
}
