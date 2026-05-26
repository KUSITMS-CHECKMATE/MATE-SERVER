package server.MATE.domain.report.service.excel.preparer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.entity.TreeTest;
import server.MATE.domain.report.excel.treetest.TreeTestReportExcelData;
import server.MATE.domain.report.service.excel.support.ReportExcelExportContext;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class TreeTestReportExcelServiceTest {

    @InjectMocks
    private TreeTestReportExcelService treeTestReportExcelService;

    @Test
    void prepareData는_트리_테스트_응답과_통계를_엑셀_데이터로_조립한다() {
        Question question = Question.builder()
                .testId(10L)
                .questionType(QuestionType.TREE_TEST)
                .title("고객센터를 찾아보세요.")
                .sequence(1L)
                .build();
        ReflectionTestUtils.setField(question, "id", 20L);

        TreeTest customerCenter = mock(TreeTest.class);
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

        ReportExcelExportContext context = ReportExcelExportContext.builder()
                .questionById(Map.of(20L, question))
                .reportResultByQuestionId(Map.of(20L, reportResult))
                .answersByQuestionId(Map.of(20L, List.of(answer)))
                .treeNodeByIdByQuestionId(Map.of(20L, Map.of(12L, customerCenter)))
                .build();

        TreeTestReportExcelData data = treeTestReportExcelService.prepareData(context, 20L);

        assertThat(data.respondents()).hasSize(1);
        assertThat(data.pathStats()).hasSize(1);
        assertThat(data.totalResponseCount()).isEqualTo(1);
    }

    @Test
    void 트리_테스트가_아니면_prepareData를_허용하지_않는다() {
        Question question = Question.builder()
                .testId(10L)
                .questionType(QuestionType.OBJECTIVE)
                .title("객관식")
                .sequence(1L)
                .build();
        ReflectionTestUtils.setField(question, "id", 20L);

        ReportExcelExportContext context = ReportExcelExportContext.builder()
                .questionById(Map.of(20L, question))
                .build();

        assertThatThrownBy(() -> treeTestReportExcelService.prepareData(context, 20L))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(BaseErrorCode.REPORT_008));
    }
}
