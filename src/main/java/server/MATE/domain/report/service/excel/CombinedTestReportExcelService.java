package server.MATE.domain.report.service.excel;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.question.dto.response.QuestionSummaryItem;
import server.MATE.domain.question.entity.FiveSecond;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.report.dto.response.TestReportExcelDownload;
import server.MATE.domain.report.excel.abtest.AbTestReportExcelData;
import server.MATE.domain.report.excel.abtest.AbTestReportExcelWriter;
import server.MATE.domain.report.excel.cardsorting.CardSortingReportExcelData;
import server.MATE.domain.report.excel.cardsorting.CardSortingReportExcelWriter;
import server.MATE.domain.report.excel.fivesecond.FiveSecondObjectiveReportExcelData;
import server.MATE.domain.report.excel.fivesecond.FiveSecondObjectiveReportExcelWriter;
import server.MATE.domain.report.excel.fivesecond.FiveSecondSubjectiveReportExcelData;
import server.MATE.domain.report.excel.fivesecond.FiveSecondSubjectiveReportExcelWriter;
import server.MATE.domain.report.excel.basic.MateReportExcelWriter;
import server.MATE.domain.report.excel.master.MasterTemplateReportExcelWriter;
import server.MATE.domain.report.excel.objective.ObjectiveReportExcelData;
import server.MATE.domain.report.excel.objective.ObjectiveReportExcelWriter;
import server.MATE.domain.report.excel.common.ReportExcelWriteMode;
import server.MATE.domain.report.excel.scale.ScaleReportExcelData;
import server.MATE.domain.report.excel.scale.ScaleReportExcelWriter;
import server.MATE.domain.report.excel.subjective.SubjectiveReportExcelData;
import server.MATE.domain.report.excel.subjective.SubjectiveReportExcelWriter;
import server.MATE.domain.report.excel.basic.TestReportExcelData;
import server.MATE.domain.report.excel.treetest.TreeTestReportExcelData;
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
import server.MATE.domain.test.entity.Test;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private final ReportExcelExportContextLoader reportExcelExportContextLoader;
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
        ReportExcelExportContext context = reportExcelExportContextLoader.load(testId, makerId);
        Test test = context.test();
        List<QuestionSummaryItem> questions = context.questionSummaries();
        if (questions.size() > MateReportExcelWriter.MAX_QUESTION_ROWS) {
            throw new BaseException(BaseErrorCode.REPORT_001);
        }

        ExportSession session = new ExportSession(context, new HashMap<>());

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
            writeMasterTemplateSheet(workbook.createSheet(SHEET_MASTER_TEMPLATE), session, questions);

            writeQuestionTypeSheet(workbook, SHEET_OBJECTIVE, QuestionType.OBJECTIVE, session, questions);
            writeQuestionTypeSheet(workbook, SHEET_SUBJECTIVE, QuestionType.SUBJECTIVE, session, questions);
            writeQuestionTypeSheet(workbook, SHEET_AB_TEST, QuestionType.AB_TEST, session, questions);
            writeQuestionTypeSheet(workbook, SHEET_SCALE, QuestionType.SCALE, session, questions);
            writeQuestionTypeSheet(workbook, SHEET_CARD_SORTING, QuestionType.CARD_SORTING, session, questions);
            writeQuestionTypeSheet(workbook, SHEET_TREE_TEST, QuestionType.TREE_TEST, session, questions);
            writeFiveSecondSheet(workbook.createSheet(SHEET_FIVE_SECOND), session, questions);

            workbook.write(outputStream);
            return new TestReportExcelDownload(outputStream.toByteArray(), buildFilename(testId));
        } catch (IOException e) {
            throw new UncheckedIOException("통합 엑셀 보고서 생성에 실패했습니다.", e);
        }
    }

    private void writeMasterTemplateSheet(
            Sheet sheet,
            ExportSession session,
            List<QuestionSummaryItem> questions
    ) {
        masterTemplateReportExcelWriter.configureSheet(sheet);
        int rowIndex = masterTemplateReportExcelWriter.writeGlobalHeader(sheet, 0);

        rowIndex = appendMasterTemplateTypeSection(sheet, rowIndex, QuestionType.OBJECTIVE, session, questions);
        rowIndex = appendMasterTemplateTypeSection(sheet, rowIndex, QuestionType.SUBJECTIVE, session, questions);
        rowIndex = appendMasterTemplateTypeSection(sheet, rowIndex, QuestionType.AB_TEST, session, questions);
        rowIndex = appendMasterTemplateTypeSection(sheet, rowIndex, QuestionType.SCALE, session, questions);
        rowIndex = appendMasterTemplateTypeSection(sheet, rowIndex, QuestionType.CARD_SORTING, session, questions);
        rowIndex = appendMasterTemplateTypeSection(sheet, rowIndex, QuestionType.TREE_TEST, session, questions);
        appendMasterTemplateFiveSecondSection(sheet, rowIndex, session, questions);
    }

    private int appendMasterTemplateTypeSection(
            Sheet sheet,
            int startRowIndex,
            QuestionType questionType,
            ExportSession session,
            List<QuestionSummaryItem> questions
    ) {
        List<QuestionSummaryItem> typedQuestions = questions.stream()
                .filter(question -> question.type() == questionType)
                .toList();

        if (typedQuestions.isEmpty()) {
            return startRowIndex;
        }

        int rowIndex = startRowIndex;
        for (int index = 0; index < typedQuestions.size(); index++) {
            QuestionSummaryItem question = typedQuestions.get(index);
            rowIndex = writeQuestionBlock(
                    sheet,
                    rowIndex,
                    questionType,
                    session,
                    question.questionId(),
                    ReportExcelWriteMode.MASTER_SECTION
            );
            if (index < typedQuestions.size() - 1) {
                rowIndex += SECTION_GAP_ROWS;
            }
        }
        return rowIndex + SECTION_GAP_ROWS;
    }

    private void appendMasterTemplateFiveSecondSection(
            Sheet sheet,
            int startRowIndex,
            ExportSession session,
            List<QuestionSummaryItem> questions
    ) {
        writeFiveSecondBlocks(sheet, startRowIndex, session, questions, ReportExcelWriteMode.MASTER_SECTION);
    }

    private void writeQuestionTypeSheet(
            XSSFWorkbook workbook,
            String sheetName,
            QuestionType questionType,
            ExportSession session,
            List<QuestionSummaryItem> questions
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
            rowIndex = writeQuestionBlock(
                    sheet,
                    rowIndex,
                    questionType,
                    session,
                    question.questionId(),
                    ReportExcelWriteMode.STANDALONE
            );
            if (index < typedQuestions.size() - 1) {
                rowIndex += SECTION_GAP_ROWS;
            }
        }
    }

    private void writeFiveSecondSheet(
            Sheet sheet,
            ExportSession session,
            List<QuestionSummaryItem> questions
    ) {
        List<QuestionSummaryItem> fiveSecondQuestions = questions.stream()
                .filter(question -> question.type() == QuestionType.FIVE_SECOND)
                .toList();

        if (fiveSecondQuestions.isEmpty()) {
            writeEmptySheetMessage(sheet, "해당 유형의 질문이 없습니다.");
            return;
        }

        writeFiveSecondBlocks(sheet, 0, session, questions, ReportExcelWriteMode.STANDALONE);
    }

    private void writeFiveSecondBlocks(
            Sheet sheet,
            int startRowIndex,
            ExportSession session,
            List<QuestionSummaryItem> questions,
            ReportExcelWriteMode mode
    ) {
        List<QuestionSummaryItem> fiveSecondQuestions = questions.stream()
                .filter(question -> question.type() == QuestionType.FIVE_SECOND)
                .toList();

        if (fiveSecondQuestions.isEmpty()) {
            return;
        }

        int rowIndex = startRowIndex;
        for (int index = 0; index < fiveSecondQuestions.size(); index++) {
            QuestionSummaryItem question = fiveSecondQuestions.get(index);
            Object preparedData = getFiveSecondPreparedData(session, question.questionId());
            if (preparedData instanceof FiveSecondObjectiveReportExcelData objectiveData) {
                rowIndex = fiveSecondObjectiveReportExcelWriter.writeToSheet(sheet, rowIndex, objectiveData, mode);
            } else if (preparedData instanceof FiveSecondSubjectiveReportExcelData subjectiveData) {
                rowIndex = fiveSecondSubjectiveReportExcelWriter.writeToSheet(sheet, rowIndex, subjectiveData, mode);
            } else {
                throw new BaseException(BaseErrorCode.COMMON_002);
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
            ExportSession session,
            Long questionId,
            ReportExcelWriteMode mode
    ) {
        return switch (questionType) {
            case OBJECTIVE -> objectiveReportExcelWriter.writeToSheet(
                    sheet,
                    startRowIndex,
                    getObjectivePreparedData(session, questionId),
                    mode
            );
            case SUBJECTIVE -> subjectiveReportExcelWriter.writeToSheet(
                    sheet,
                    startRowIndex,
                    getSubjectivePreparedData(session, questionId),
                    mode
            );
            case AB_TEST -> abTestReportExcelWriter.writeToSheet(
                    sheet,
                    startRowIndex,
                    getAbTestPreparedData(session, questionId),
                    mode
            );
            case SCALE -> scaleReportExcelWriter.writeToSheet(
                    sheet,
                    startRowIndex,
                    getScalePreparedData(session, questionId),
                    mode
            );
            case CARD_SORTING -> cardSortingReportExcelWriter.writeToSheet(
                    sheet,
                    startRowIndex,
                    getCardSortingPreparedData(session, questionId),
                    mode
            );
            case TREE_TEST -> treeTestReportExcelWriter.writeToSheet(
                    sheet,
                    startRowIndex,
                    getTreeTestPreparedData(session, questionId),
                    mode
            );
            default -> throw new BaseException(BaseErrorCode.COMMON_002);
        };
    }

    private ObjectiveReportExcelData getObjectivePreparedData(ExportSession session, Long questionId) {
        return (ObjectiveReportExcelData) session.preparedDataByQuestionId().computeIfAbsent(
                questionId,
                id -> objectiveReportExcelService.prepareData(session.context(), id)
        );
    }

    private SubjectiveReportExcelData getSubjectivePreparedData(ExportSession session, Long questionId) {
        return (SubjectiveReportExcelData) session.preparedDataByQuestionId().computeIfAbsent(
                questionId,
                id -> subjectiveReportExcelService.prepareData(session.context(), id)
        );
    }

    private AbTestReportExcelData getAbTestPreparedData(ExportSession session, Long questionId) {
        return (AbTestReportExcelData) session.preparedDataByQuestionId().computeIfAbsent(
                questionId,
                id -> abTestReportExcelService.prepareData(session.context(), id)
        );
    }

    private ScaleReportExcelData getScalePreparedData(ExportSession session, Long questionId) {
        return (ScaleReportExcelData) session.preparedDataByQuestionId().computeIfAbsent(
                questionId,
                id -> scaleReportExcelService.prepareData(session.context(), id)
        );
    }

    private CardSortingReportExcelData getCardSortingPreparedData(ExportSession session, Long questionId) {
        return (CardSortingReportExcelData) session.preparedDataByQuestionId().computeIfAbsent(
                questionId,
                id -> cardSortingReportExcelService.prepareData(session.context(), id)
        );
    }

    private TreeTestReportExcelData getTreeTestPreparedData(ExportSession session, Long questionId) {
        return (TreeTestReportExcelData) session.preparedDataByQuestionId().computeIfAbsent(
                questionId,
                id -> treeTestReportExcelService.prepareData(session.context(), id)
        );
    }

    private Object getFiveSecondPreparedData(ExportSession session, Long questionId) {
        return session.preparedDataByQuestionId().computeIfAbsent(questionId, id -> {
            FiveSecond fiveSecond = session.context().requireFiveSecond(id);
            if (fiveSecond.isObjective()) {
                return fiveSecondReportExcelService.prepareObjectiveData(session.context(), id);
            }
            return fiveSecondReportExcelService.prepareSubjectiveData(session.context(), id);
        });
    }

    private void writeEmptySheetMessage(Sheet sheet, String message) {
        sheet.createRow(0).createCell(0).setCellValue(message);
    }

    private String formatTestPeriod(Test test) {
        if (test.getCreatedAt() == null || test.getClosedAt() == null) {
            return "";
        }

        String start = DATE_FORMAT.format(test.getCreatedAt());
        String end = DATE_FORMAT.format(test.getClosedAt());
        return start + " ~ " + end;
    }

    private String buildFilename(Long testId) {
        return "mate-report-" + testId + ".xlsx";
    }

    private record ExportSession(
            ReportExcelExportContext context,
            Map<Long, Object> preparedDataByQuestionId
    ) {
    }
}
