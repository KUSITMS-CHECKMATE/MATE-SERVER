package server.MATE.domain.report.excel.fivesecond;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import server.MATE.domain.report.excel.common.ReportExcelWriteMode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

@Component
public class FiveSecondObjectiveReportExcelWriter {

    private static final int LAST_COLUMN = 6;
    private static final int LEFT_LAST_COLUMN = 2;
    private static final int STAT_CONTENT_COLUMN = 4;
    private static final int STAT_COUNT_COLUMN = 5;
    private static final int STAT_RATIO_COLUMN = 6;
    private static final int LEFT_COLUMN_WIDTH = 18 * 256;
    private static final int SHARED_COLUMN_WIDTH = 16 * 256;

    private static final String SHEET_NAME = "5초 테스트 통계";

    public byte[] write(FiveSecondObjectiveReportExcelData data) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            writeToSheet(sheet, 0, data);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("5초 테스트(객관식) 통계 엑셀 생성에 실패했습니다.", e);
        }
    }

    public int writeToSheet(Sheet sheet, int startRowIndex, FiveSecondObjectiveReportExcelData data) {
        return writeToSheet(sheet, startRowIndex, data, ReportExcelWriteMode.STANDALONE);
    }

    public int writeToSheet(
            Sheet sheet,
            int startRowIndex,
            FiveSecondObjectiveReportExcelData data,
            ReportExcelWriteMode mode
    ) {
        if (startRowIndex == 0 && mode == ReportExcelWriteMode.STANDALONE) {
            configureColumnWidths(sheet);
        }
        FiveSecondReportExcelStyles styles = FiveSecondReportExcelStyles.create(sheet.getWorkbook());

        int rowIndex = startRowIndex;
        if (mode == ReportExcelWriteMode.STANDALONE) {
            rowIndex = writeQuestionSettingHeader(sheet, rowIndex, data.questionNumberLabel(), styles);
            rowIndex = writeQuestionMetaRow(sheet, rowIndex, data.questionTitle(), styles);
            rowIndex++;
        }
        rowIndex = writeSectionTitleRow(sheet, rowIndex, styles);
        rowIndex = writeQuestionRow(sheet, rowIndex, data.questionTitle(), styles);
        rowIndex = writeQuestionTypeRow(sheet, rowIndex, "객관식", styles);
        rowIndex = writeTableHeaderRow(sheet, rowIndex, styles);
        return writeDataRows(sheet, rowIndex, data, styles);
    }

    private void configureColumnWidths(Sheet sheet) {
        sheet.setColumnWidth(0, LEFT_COLUMN_WIDTH);
        for (int columnIndex = 1; columnIndex <= LAST_COLUMN; columnIndex++) {
            sheet.setColumnWidth(columnIndex, SHARED_COLUMN_WIDTH);
        }
    }

    private int writeQuestionSettingHeader(
            Sheet sheet,
            int rowIndex,
            String questionNumberLabel,
            FiveSecondReportExcelStyles styles
    ) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(24f);
        createCell(row, 0, questionNumberLabel + " - 질문 설정", styles.questionSettingHeader());
        mergeRow(sheet, rowIndex, 0, LAST_COLUMN, styles.questionSettingHeader());
        return rowIndex + 1;
    }

    private int writeQuestionMetaRow(
            Sheet sheet,
            int rowIndex,
            String questionTitle,
            FiveSecondReportExcelStyles styles
    ) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(22f);

        createCell(row, 0, "질문 유형", styles.label());
        createCell(row, 1, "5초 테스트", styles.value());
        mergeRow(sheet, rowIndex, 1, LEFT_LAST_COLUMN, styles.value());
        createCell(row, STAT_CONTENT_COLUMN, "질문 제목", styles.label());
        createCell(row, STAT_COUNT_COLUMN, questionTitle == null ? "" : questionTitle, styles.value());
        mergeRow(sheet, rowIndex, STAT_COUNT_COLUMN, LAST_COLUMN, styles.value());
        return rowIndex + 1;
    }

    private int writeSectionTitleRow(Sheet sheet, int rowIndex, FiveSecondReportExcelStyles styles) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(24f);

        createCell(row, 0, "5초 테스트", styles.sectionHeader());
        mergeRow(sheet, rowIndex, 0, LEFT_LAST_COLUMN, styles.sectionHeader());

        createCell(row, STAT_CONTENT_COLUMN, "📊 5초 테스트 - 통계", styles.statsSectionHeader());
        mergeRow(sheet, rowIndex, STAT_CONTENT_COLUMN, LAST_COLUMN, styles.statsSectionHeader());
        return rowIndex + 1;
    }

    private int writeQuestionRow(
            Sheet sheet,
            int rowIndex,
            String questionTitle,
            FiveSecondReportExcelStyles styles
    ) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(22f);

        createCell(row, 0, "질문", styles.questionLabel());
        createCell(row, 1, questionTitle == null ? "" : questionTitle, styles.data());
        mergeRow(sheet, rowIndex, 1, LEFT_LAST_COLUMN, styles.data());
        return rowIndex + 1;
    }

    private int writeQuestionTypeRow(
            Sheet sheet,
            int rowIndex,
            String questionMode,
            FiveSecondReportExcelStyles styles
    ) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(22f);

        createCell(row, 0, "질문 유형 (객관/주관)", styles.questionLabel());
        createCell(row, 1, questionMode, styles.data());
        mergeRow(sheet, rowIndex, 1, LEFT_LAST_COLUMN, styles.data());

        createCell(row, STAT_CONTENT_COLUMN, "응답 내용", styles.tableHeader());
        createCell(row, STAT_COUNT_COLUMN, "응답 수", styles.tableHeader());
        createCell(row, STAT_RATIO_COLUMN, "비율 (%)", styles.tableHeader());
        return rowIndex + 1;
    }

    private int writeTableHeaderRow(Sheet sheet, int rowIndex, FiveSecondReportExcelStyles styles) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(22f);

        createCell(row, 0, "응답자 번호", styles.tableHeader());
        createCell(row, 1, "응답 내용", styles.tableHeader());
        mergeRow(sheet, rowIndex, 1, LEFT_LAST_COLUMN, styles.tableHeader());
        return rowIndex + 1;
    }

    private int writeDataRows(
            Sheet sheet,
            int rowIndex,
            FiveSecondObjectiveReportExcelData data,
            FiveSecondReportExcelStyles styles
    ) {
        List<FiveSecondRespondentRow> respondents = data.respondents();
        List<FiveSecondOptionStatRow> optionStats = data.optionStats();
        int rowCount = Math.max(respondents.size(), optionStats.size());

        for (int relativeRow = 0; relativeRow < rowCount; relativeRow++) {
            Row row = sheet.createRow(rowIndex + relativeRow);
            row.setHeightInPoints(22f);
            writeRespondentCells(sheet, row, relativeRow, respondents, styles);
            writeStatCells(row, relativeRow, optionStats, styles);
        }

        Row totalRow = sheet.createRow(rowIndex + rowCount);
        totalRow.setHeightInPoints(22f);
        createCell(totalRow, STAT_CONTENT_COLUMN, "합계", styles.totalLabel());
        createCell(totalRow, STAT_COUNT_COLUMN, String.valueOf(data.totalResponses()), styles.data());
        createCell(totalRow, STAT_RATIO_COLUMN, data.totalResponses() == 0 ? "-" : "100", styles.data());
        return rowIndex + rowCount + 1;
    }

    private void writeRespondentCells(
            Sheet sheet,
            Row row,
            int index,
            List<FiveSecondRespondentRow> respondents,
            FiveSecondReportExcelStyles styles
    ) {
        if (index < respondents.size()) {
            FiveSecondRespondentRow respondent = respondents.get(index);
            createCell(row, 0, String.valueOf(respondent.respondentNumber()), styles.data());
            createCell(row, 1, respondent.answerContent(), styles.data());
            mergeRow(sheet, row.getRowNum(), 1, LEFT_LAST_COLUMN, styles.data());
            return;
        }
        createCell(row, 0, "", styles.data());
        createCell(row, 1, "", styles.data());
        mergeRow(sheet, row.getRowNum(), 1, LEFT_LAST_COLUMN, styles.data());
    }

    private void writeStatCells(
            Row row,
            int index,
            List<FiveSecondOptionStatRow> optionStats,
            FiveSecondReportExcelStyles styles
    ) {
        if (index < optionStats.size()) {
            FiveSecondOptionStatRow stat = optionStats.get(index);
            createCell(row, STAT_CONTENT_COLUMN, stat.responseContent(), styles.data());
            createCell(row, STAT_COUNT_COLUMN, String.valueOf(stat.responseCount()), styles.data());
            createCell(row, STAT_RATIO_COLUMN, stat.ratioPercent(), styles.data());
            return;
        }
        createCell(row, STAT_CONTENT_COLUMN, "", styles.data());
        createCell(row, STAT_COUNT_COLUMN, "", styles.data());
        createCell(row, STAT_RATIO_COLUMN, "", styles.data());
    }

    private void createCell(Row row, int columnIndex, String value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void mergeRow(Sheet sheet, int rowIndex, int firstCol, int lastCol, CellStyle style) {
        if (firstCol == lastCol) {
            return;
        }
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, firstCol, lastCol));
        Row row = sheet.getRow(rowIndex);
        for (int columnIndex = firstCol + 1; columnIndex <= lastCol; columnIndex++) {
            Cell cell = row.getCell(columnIndex);
            if (cell == null) {
                cell = row.createCell(columnIndex);
            }
            cell.setCellStyle(style);
        }
    }
}
