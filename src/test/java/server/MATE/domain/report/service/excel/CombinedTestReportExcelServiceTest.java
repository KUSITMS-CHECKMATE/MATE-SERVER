package server.MATE.domain.report.service.excel;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import server.MATE.domain.question.dto.response.QuestionSummaryItem;
import server.MATE.domain.question.entity.FiveSecond;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.report.dto.response.TestReportExcelDownload;
import server.MATE.domain.report.excel.abtest.AbTestReportExcelWriter;
import server.MATE.domain.report.excel.fivesecond.FiveSecondObjectiveReportExcelData;
import server.MATE.domain.report.excel.fivesecond.FiveSecondObjectiveReportExcelWriter;
import server.MATE.domain.report.excel.fivesecond.FiveSecondSubjectiveReportExcelWriter;
import server.MATE.domain.report.excel.basic.MateReportExcelWriter;
import server.MATE.domain.report.excel.cardsorting.CardSortingReportExcelWriter;
import server.MATE.domain.report.excel.master.MasterTemplateReportExcelWriter;
import server.MATE.domain.report.excel.objective.ObjectiveReportExcelData;
import server.MATE.domain.report.excel.objective.ObjectiveReportExcelWriter;
import server.MATE.domain.report.excel.scale.ScaleReportExcelWriter;
import server.MATE.domain.report.excel.subjective.SubjectiveReportExcelWriter;
import server.MATE.domain.report.excel.treetest.TreeTestReportExcelWriter;
import server.MATE.domain.report.service.excel.preparer.AbTestReportExcelService;
import server.MATE.domain.report.service.excel.preparer.CardSortingReportExcelService;
import server.MATE.domain.report.service.excel.preparer.FiveSecondReportExcelService;
import server.MATE.domain.report.service.excel.preparer.ObjectiveReportExcelService;
import server.MATE.domain.report.service.excel.preparer.ScaleReportExcelService;
import server.MATE.domain.report.service.excel.preparer.SubjectiveReportExcelService;
import server.MATE.domain.report.service.excel.preparer.TreeTestReportExcelService;
import server.MATE.domain.report.service.excel.support.ReportExcelExportContext;
import server.MATE.domain.report.service.excel.support.ReportExcelExportContextLoader;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CombinedTestReportExcelServiceTest {

    private static final Long TEST_ID = 10L;
    private static final Long MAKER_ID = 1L;
    private static final Long OBJECTIVE_QUESTION_ID = 101L;
    private static final Long FIVE_SECOND_QUESTION_ID = 107L;

    @Mock
    private ReportExcelExportContextLoader reportExcelExportContextLoader;
    @Mock
    private ObjectiveReportExcelService objectiveReportExcelService;
    @Mock
    private SubjectiveReportExcelService subjectiveReportExcelService;
    @Mock
    private AbTestReportExcelService abTestReportExcelService;
    @Mock
    private ScaleReportExcelService scaleReportExcelService;
    @Mock
    private CardSortingReportExcelService cardSortingReportExcelService;
    @Mock
    private TreeTestReportExcelService treeTestReportExcelService;
    @Mock
    private FiveSecondReportExcelService fiveSecondReportExcelService;

    private CombinedTestReportExcelService combinedTestReportExcelService;

    @BeforeEach
    void setUp() {
        combinedTestReportExcelService = new CombinedTestReportExcelService(
                reportExcelExportContextLoader,
                new MateReportExcelWriter(),
                new MasterTemplateReportExcelWriter(),
                objectiveReportExcelService,
                new ObjectiveReportExcelWriter(),
                subjectiveReportExcelService,
                new SubjectiveReportExcelWriter(),
                abTestReportExcelService,
                new AbTestReportExcelWriter(),
                scaleReportExcelService,
                new ScaleReportExcelWriter(),
                cardSortingReportExcelService,
                new CardSortingReportExcelWriter(),
                treeTestReportExcelService,
                new TreeTestReportExcelWriter(),
                fiveSecondReportExcelService,
                new FiveSecondObjectiveReportExcelWriter(),
                new FiveSecondSubjectiveReportExcelWriter()
        );
    }

    @Test
    void export는_9개_시트와_기본정보_테스트기간을_포함한_xlsx를_생성한다() throws Exception {
        ReportExcelExportContext context = exportContext(LocalDateTime.of(2026, 5, 1, 10, 0));
        given(reportExcelExportContextLoader.load(TEST_ID, MAKER_ID)).willReturn(context);
        stubObjectiveData(context);
        stubFiveSecondObjectiveData(context);

        TestReportExcelDownload download = combinedTestReportExcelService.export(TEST_ID, MAKER_ID);

        assertThat(download.filename()).isEqualTo("mate-report-10.xlsx");
        assertThat(download.content()).isNotEmpty();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(download.content()))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(9);
            assertThat(workbook.getSheetName(0)).isEqualTo(CombinedTestReportExcelService.SHEET_BASIC_INFO);
            assertThat(workbook.getSheetName(8)).isEqualTo(CombinedTestReportExcelService.SHEET_FIVE_SECOND);

            var basicInfoSheet = workbook.getSheetAt(0);
            assertThat(basicInfoSheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("통합 테스트");
            assertThat(basicInfoSheet.getRow(3).getCell(1).getStringCellValue()).isEqualTo("2026.05.01 ~ 2026.06.01");

            var objectiveSheet = workbook.getSheetAt(2);
            assertThat(objectiveSheet.getRow(0).getCell(0).getStringCellValue()).contains("Q01 - 질문 설정");

            var subjectiveSheet = workbook.getSheetAt(3);
            assertThat(subjectiveSheet.getRow(0).getCell(0).getStringCellValue())
                    .isEqualTo("해당 유형의 질문이 없습니다.");
        }
    }

    @Test
    void export는_같은_질문에_대해_preparer를_한_번만_호출한다() {
        List<QuestionSummaryItem> questions = List.of(
                new QuestionSummaryItem(OBJECTIVE_QUESTION_ID, 1L, "객관식 질문", QuestionType.OBJECTIVE)
        );
        ReportExcelExportContext context = ReportExcelExportContext.builder()
                .test(exportReadyTest(LocalDateTime.of(2026, 5, 1, 10, 0)))
                .testId(TEST_ID)
                .questionSummaries(questions)
                .build();
        given(reportExcelExportContextLoader.load(TEST_ID, MAKER_ID)).willReturn(context);
        stubObjectiveData(context);

        combinedTestReportExcelService.export(TEST_ID, MAKER_ID);

        verify(objectiveReportExcelService, times(1))
                .prepareData(context, OBJECTIVE_QUESTION_ID);
    }

    @Test
    void 질문이_21개면_REPORT_001을_던진다() {
        List<QuestionSummaryItem> questions = new ArrayList<>();
        for (long index = 1; index <= 21; index++) {
            questions.add(new QuestionSummaryItem(index, index, "질문 " + index, QuestionType.OBJECTIVE));
        }

        ReportExcelExportContext context = ReportExcelExportContext.builder()
                .test(exportReadyTest(LocalDateTime.of(2026, 5, 1, 10, 0)))
                .testId(TEST_ID)
                .questionSummaries(questions)
                .build();
        given(reportExcelExportContextLoader.load(TEST_ID, MAKER_ID)).willReturn(context);

        assertThatThrownBy(() -> combinedTestReportExcelService.export(TEST_ID, MAKER_ID))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(BaseErrorCode.REPORT_001));
    }

    private ReportExcelExportContext exportContext(LocalDateTime createdAt) {
        List<QuestionSummaryItem> questions = List.of(
                new QuestionSummaryItem(OBJECTIVE_QUESTION_ID, 1L, "객관식 질문", QuestionType.OBJECTIVE),
                new QuestionSummaryItem(FIVE_SECOND_QUESTION_ID, 2L, "5초 객관식", QuestionType.FIVE_SECOND)
        );

        Question fiveSecondQuestion = Question.builder()
                .testId(TEST_ID)
                .questionType(QuestionType.FIVE_SECOND)
                .title("5초 객관식")
                .sequence(2L)
                .build();
        ReflectionTestUtils.setField(fiveSecondQuestion, "id", FIVE_SECOND_QUESTION_ID);

        FiveSecond fiveSecond = FiveSecond.builder()
                .question(fiveSecondQuestion)
                .imageKey("image-key")
                .isObjective(true)
                .build();
        ReflectionTestUtils.setField(fiveSecond, "id", FIVE_SECOND_QUESTION_ID);

        return ReportExcelExportContext.builder()
                .test(exportReadyTest(createdAt))
                .testId(TEST_ID)
                .questionSummaries(questions)
                .fiveSecondByQuestionId(Map.of(FIVE_SECOND_QUESTION_ID, fiveSecond))
                .build();
    }

    private server.MATE.domain.test.entity.Test exportReadyTest(LocalDateTime createdAt) {
        server.MATE.domain.test.entity.Test test = server.MATE.domain.test.entity.Test.builder()
                .makerId(MAKER_ID)
                .title("통합 테스트")
                .description("설명")
                .goalPpl(100)
                .testStatus(TestStatus.COMPLETED)
                .closedAt(LocalDateTime.of(2026, 6, 1, 23, 59, 59))
                .build();
        ReflectionTestUtils.setField(test, "createdAt", createdAt);
        return test;
    }

    private void stubObjectiveData(ReportExcelExportContext context) {
        given(objectiveReportExcelService.prepareData(eq(context), eq(OBJECTIVE_QUESTION_ID)))
                .willReturn(new ObjectiveReportExcelData(
                        "Q01",
                        "객관식 질문",
                        List.of(),
                        List.of(),
                        0
                ));
    }

    private void stubFiveSecondObjectiveData(ReportExcelExportContext context) {
        given(fiveSecondReportExcelService.prepareObjectiveData(eq(context), eq(FIVE_SECOND_QUESTION_ID)))
                .willReturn(new FiveSecondObjectiveReportExcelData(
                        "Q02",
                        "5초 객관식",
                        List.of(),
                        List.of(),
                        0
                ));
    }
}
