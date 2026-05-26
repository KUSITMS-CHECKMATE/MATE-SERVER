package server.MATE.domain.report.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.question.dto.response.QuestionSummaryItem;
import server.MATE.domain.question.entity.FiveSecond;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.FiveSecondRepository;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.report.dto.response.TestReportExcelDownload;
import server.MATE.domain.report.excel.AbTestReportExcelWriter;
import server.MATE.domain.report.excel.CardSortingReportExcelWriter;
import server.MATE.domain.report.excel.FiveSecondObjectiveReportExcelWriter;
import server.MATE.domain.report.excel.FiveSecondSubjectiveReportExcelWriter;
import server.MATE.domain.report.excel.MateReportExcelWriter;
import server.MATE.domain.report.excel.MasterTemplateReportExcelWriter;
import server.MATE.domain.report.excel.ObjectiveReportExcelWriter;
import server.MATE.domain.report.excel.ScaleReportExcelWriter;
import server.MATE.domain.report.excel.SubjectiveReportExcelWriter;
import server.MATE.domain.report.excel.TestReportExcelData;
import server.MATE.domain.report.excel.TreeTestReportExcelWriter;
import server.MATE.domain.test.entity.Test;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CombinedTestReportExcelService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final int SECTION_GAP_ROWS = 2;

    static final String SHEET_BASIC_INFO = "기본 정보";
    static final String SHEET_MASTER_TEMPLATE = "마스터 템플릿";
    static final String SHEET_OBJECTIVE = "객관식";
    static final String SHEET_SUBJECTIVE = "주관식";
    static final String SHEET_AB_TEST = "AB 테스트";
    static final String SHEET_SCALE = "척도 테스트";
    static final String SHEET_CARD_SORTING = "카드소팅";
    static final String SHEET_TREE_TEST = "트리테스트";
    static final String SHEET_FIVE_SECOND = "5초 테스트";

    private final ReportExcelExportSupport reportExcelExportSupport;
    private final QuestionRepository questionRepository;
    private final FiveSecondRepository fiveSecondRepository;
    private final MateReportExcelWriter mateReportExcelWriter;
    private final MasterTemplateReportExcelWriter masterTemplateReportExcelWriter;
    private final ObjectiveReportExcelService objectiveReportExcelService;
    private final ObjectiveReportExcelWriter objectiveReportExcelWriter;
    private final SubjectiveReportExcelService subjectiveReportExcelService;
    private final SubjectiveReportExcelWriter subjectiveReportExcelWriter;
    private final AbTestReportExcelService abTestReportExcelService;
    private final AbTestReportExcelWriter abTestReportExcelWriter;
    private final ScaleReportExcelService scaleReportExcelService;
    private final ScaleReportExcelWriter scaleReportExcelWriter;
    private final CardSortingReportExcelService cardSortingReportExcelService;
    private final CardSortingReportExcelWriter cardSortingReportExcelWriter;
    private final TreeTestReportExcelService treeTestReportExcelService;
    private final TreeTestReportExcelWriter treeTestReportExcelWriter;
    private final FiveSecondReportExcelService fiveSecondReportExcelService;
    private final FiveSecondObjectiveReportExcelWriter fiveSecondObjectiveReportExcelWriter;
    private final FiveSecondSubjectiveReportExcelWriter fiveSecondSubjectiveReportExcelWriter;

    public TestReportExcelDownload export(Long testId, Long makerId) {
        Test test = reportExcelExportSupport.requireExportReadyTest(testId, makerId);
        List<QuestionSummaryItem> questions = questionRepository.findQuestionSummariesByTestId(testId);
        if (questions.size() > MateReportExcelWriter.MAX_QUESTION_ROWS) {
            throw new BaseException(BaseErrorCode.REPORT_001);
        }

        TestReportExcelData basicInfoData = new TestReportExcelData(
                test.getTitle(),
                test.getDescription(),
                formatTestPeriod(test),
                test.getGoalPpl(),
                questions
        );

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            mateReportExcelWriter.writeToSheet(workbook.createSheet(SHEET_BASIC_INFO), 0, basicInfoData);
            masterTemplateReportExcelWriter.writeToSheet(workbook.createSheet(SHEET_MASTER_TEMPLATE), 0);

            writeQuestionTypeSheet(workbook, SHEET_OBJECTIVE, QuestionType.OBJECTIVE, questions, testId, makerId);
            writeQuestionTypeSheet(workbook, SHEET_SUBJECTIVE, QuestionType.SUBJECTIVE, questions, testId, makerId);
            writeQuestionTypeSheet(workbook, SHEET_AB_TEST, QuestionType.AB_TEST, questions, testId, makerId);
            writeQuestionTypeSheet(workbook, SHEET_SCALE, QuestionType.SCALE, questions, testId, makerId);
            writeQuestionTypeSheet(workbook, SHEET_CARD_SORTING, QuestionType.CARD_SORTING, questions, testId, makerId);
            writeQuestionTypeSheet(workbook, SHEET_TREE_TEST, QuestionType.TREE_TEST, questions, testId, makerId);
            writeFiveSecondSheet(workbook.createSheet(SHEET_FIVE_SECOND), questions, testId, makerId);

            workbook.write(outputStream);
            return new TestReportExcelDownload(outputStream.toByteArray(), buildFilename(testId));
        } catch (IOException e) {
            throw new UncheckedIOException("통합 엑셀 보고서 생성에 실패했습니다.", e);
        }
    }

    private void writeQuestionTypeSheet(
            XSSFWorkbook workbook,
            String sheetName,
            QuestionType questionType,
            List<QuestionSummaryItem> questions,
            Long testId,
            Long makerId
    ) {
        Sheet sheet = workbook.createSheet(sheetName);
        List<QuestionSummaryItem> typedQuestions = questions.stream()
                .filter(question -> question.type() == questionType)
                .toList();

        if (typedQuestions.isEmpty()) {
            writeEmptySheetMessage(sheet, "해당 유형의 질문이 없습니다.");
            return;
        }

        int rowIndex = 0;
        for (int index = 0; index < typedQuestions.size(); index++) {
            QuestionSummaryItem question = typedQuestions.get(index);
            rowIndex = writeQuestionBlock(sheet, rowIndex, questionType, testId, question.questionId(), makerId);
            if (index < typedQuestions.size() - 1) {
                rowIndex += SECTION_GAP_ROWS;
            }
        }
    }

    private void writeFiveSecondSheet(
            Sheet sheet,
            List<QuestionSummaryItem> questions,
            Long testId,
            Long makerId
    ) {
        List<QuestionSummaryItem> fiveSecondQuestions = questions.stream()
                .filter(question -> question.type() == QuestionType.FIVE_SECOND)
                .toList();

        if (fiveSecondQuestions.isEmpty()) {
            writeEmptySheetMessage(sheet, "해당 유형의 질문이 없습니다.");
            return;
        }

        int rowIndex = 0;
        for (int index = 0; index < fiveSecondQuestions.size(); index++) {
            QuestionSummaryItem question = fiveSecondQuestions.get(index);
            FiveSecond fiveSecond = fiveSecondRepository.findWithOptionsById(question.questionId())
                    .orElseThrow(() -> new BaseException(BaseErrorCode.QUESTION_005));

            if (fiveSecond.isObjective()) {
                rowIndex = fiveSecondObjectiveReportExcelWriter.writeToSheet(
                        sheet,
                        rowIndex,
                        fiveSecondReportExcelService.prepareObjectiveData(testId, question.questionId(), makerId)
                );
            } else {
                rowIndex = fiveSecondSubjectiveReportExcelWriter.writeToSheet(
                        sheet,
                        rowIndex,
                        fiveSecondReportExcelService.prepareSubjectiveData(testId, question.questionId(), makerId)
                );
            }

            if (index < fiveSecondQuestions.size() - 1) {
                rowIndex += SECTION_GAP_ROWS;
            }
        }
    }

    private int writeQuestionBlock(
            Sheet sheet,
            int startRowIndex,
            QuestionType questionType,
            Long testId,
            Long questionId,
            Long makerId
    ) {
        return switch (questionType) {
            case OBJECTIVE -> objectiveReportExcelWriter.writeToSheet(
                    sheet,
                    startRowIndex,
                    objectiveReportExcelService.prepareData(testId, questionId, makerId)
            );
            case SUBJECTIVE -> subjectiveReportExcelWriter.writeToSheet(
                    sheet,
                    startRowIndex,
                    subjectiveReportExcelService.prepareData(testId, questionId, makerId)
            );
            case AB_TEST -> abTestReportExcelWriter.writeToSheet(
                    sheet,
                    startRowIndex,
                    abTestReportExcelService.prepareData(testId, questionId, makerId)
            );
            case SCALE -> scaleReportExcelWriter.writeToSheet(
                    sheet,
                    startRowIndex,
                    scaleReportExcelService.prepareData(testId, questionId, makerId)
            );
            case CARD_SORTING -> cardSortingReportExcelWriter.writeToSheet(
                    sheet,
                    startRowIndex,
                    cardSortingReportExcelService.prepareData(testId, questionId, makerId)
            );
            case TREE_TEST -> treeTestReportExcelWriter.writeToSheet(
                    sheet,
                    startRowIndex,
                    treeTestReportExcelService.prepareData(testId, questionId, makerId)
            );
            default -> throw new BaseException(BaseErrorCode.COMMON_002);
        };
    }

    private void writeEmptySheetMessage(Sheet sheet, String message) {
        masterTemplateReportExcelWriter.writeToSheet(sheet, 0);
        sheet.getRow(0).getCell(0).setCellValue(message);
    }

    private String formatTestPeriod(Test test) {
        if (test.getCreatedAt() == null) {
            return "";
        }

        String start = DATE_FORMAT.format(test.getCreatedAt());
        if (test.getUpdatedAt() != null) {
            return start + " ~ " + DATE_FORMAT.format(test.getUpdatedAt());
        }
        return start;
    }

    private String buildFilename(Long testId) {
        return "mate-report-" + testId + ".xlsx";
    }
}
