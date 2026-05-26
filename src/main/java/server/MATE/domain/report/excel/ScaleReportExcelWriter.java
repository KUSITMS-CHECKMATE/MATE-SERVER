package server.MATE.domain.report.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

@Component
public class ScaleReportExcelWriter {

    private static final int LAST_COLUMN = 7;
    private static final int LEFT_LAST_COLUMN = 1;
    private static final int LEFT_COLUMN_WIDTH = 18 * 256;
    private static final int SHARED_COLUMN_WIDTH = 16 * 256;
    private static final int QUESTION_TEXT_FIRST_COLUMN = 1;
    private static final int QUESTION_TEXT_LAST_COLUMN = 3;
    private static final int STAT_VALUE_COLUMN = 5;
    private static final int STAT_COUNT_COLUMN = 6;
    private static final int STAT_RATIO_COLUMN = 7;

    private static final String SHEET_NAME = "척도 테스트 통계";

    public byte[] write(ScaleReportExcelData data) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            writeToSheet(sheet, 0, data);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("척도 테스트 통계 엑셀 생성에 실패했습니다.", e);
        }
    }

    public int writeToSheet(Sheet sheet, int startRowIndex, ScaleReportExcelData data) {
        return writeToSheet(sheet, startRowIndex, data, ReportExcelWriteMode.STANDALONE);
    }

    public int writeToSheet(
            Sheet sheet,
            int startRowIndex,
            ScaleReportExcelData data,
            ReportExcelWriteMode mode
    ) {
        if (startRowIndex == 0 && mode == ReportExcelWriteMode.STANDALONE) {
            configureColumnWidths(sheet);
        }
        ScaleReportExcelStyles styles = ScaleReportExcelStyles.create(sheet.getWorkbook());

        int rowIndex = startRowIndex;
        if (mode == ReportExcelWriteMode.STANDALONE) {
            rowIndex = writeQuestionSettingHeader(sheet, rowIndex, data.questionNumberLabel(), styles);
            rowIndex = writeQuestionMetaRow(sheet, rowIndex, data.questionTitle(), styles);
            rowIndex++;
        }
        rowIndex = writeSectionTitleRow(sheet, rowIndex, styles);
        rowIndex = writeQuestionTextRow(sheet, rowIndex, data.questionTitle(), styles);
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
            ScaleReportExcelStyles styles
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
            ScaleReportExcelStyles styles
    ) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(22f);

        createCell(row, 0, "질문 유형", styles.label());
        createCell(row, 1, "척도", styles.value());
        createCell(row, STAT_VALUE_COLUMN, "질문 제목", styles.label());
        createCell(row, STAT_COUNT_COLUMN, questionTitle == null ? "" : questionTitle, styles.value());
        mergeRow(sheet, rowIndex, STAT_COUNT_COLUMN, LAST_COLUMN, styles.value());
        return rowIndex + 1;
    }

    private int writeSectionTitleRow(Sheet sheet, int rowIndex, ScaleReportExcelStyles styles) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(24f);

        createCell(row, 0, "척도 테스트", styles.sectionHeader());
        mergeRow(sheet, rowIndex, 0, LEFT_LAST_COLUMN, styles.sectionHeader());

        createCell(row, STAT_VALUE_COLUMN, "척도 테스트 - 통계", styles.sectionHeader());
        mergeRow(sheet, rowIndex, STAT_VALUE_COLUMN, LAST_COLUMN, styles.sectionHeader());
        return rowIndex + 1;
    }

    private int writeQuestionTextRow(
            Sheet sheet,
            int rowIndex,
            String questionTitle,
            ScaleReportExcelStyles styles
    ) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(28f);

        createCell(row, 0, "질문", styles.questionLabel());
        createCell(row, 1, questionTitle == null ? "" : questionTitle, styles.questionText());
        mergeRow(sheet, rowIndex, QUESTION_TEXT_FIRST_COLUMN, QUESTION_TEXT_LAST_COLUMN, styles.questionText());
        return rowIndex + 1;
    }

    private int writeTableHeaderRow(Sheet sheet, int rowIndex, ScaleReportExcelStyles styles) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(22f);

        createCell(row, 0, "응답자 번호", styles.tableHeader());
        createCell(row, 1, "응답", styles.tableHeader());
        createCell(row, STAT_VALUE_COLUMN, "응답값", styles.tableHeader());
        createCell(row, STAT_COUNT_COLUMN, "응답 수", styles.tableHeader());
        createCell(row, STAT_RATIO_COLUMN, "비율 (%)", styles.tableHeader());
        return rowIndex + 1;
    }

    private int writeDataRows(
            Sheet sheet,
            int rowIndex,
            ScaleReportExcelData data,
            ScaleReportExcelStyles styles
    ) {
        List<ScaleRespondentRow> respondents = data.respondents();
        List<ScaleValueStatRow> valueStats = data.valueStats();
        int statRowCount = valueStats.size();
        int rowCount = Math.max(respondents.size(), statRowCount + 1);

        for (int relativeRow = 0; relativeRow < rowCount; relativeRow++) {
            Row row = sheet.createRow(rowIndex + relativeRow);
            row.setHeightInPoints(22f);
            writeRespondentCells(row, relativeRow, respondents, styles);

            if (relativeRow < statRowCount) {
                writeStatCells(row, valueStats.get(relativeRow), styles);
            } else if (relativeRow == statRowCount) {
                createCell(row, STAT_VALUE_COLUMN, "평균", styles.averageLabel());
                createCell(row, STAT_COUNT_COLUMN, data.average(), styles.data());
                createCell(row, STAT_RATIO_COLUMN, "", styles.data());
            }
        }
        return rowIndex + rowCount;
    }

    private void writeRespondentCells(
            Row row,
            int index,
            List<ScaleRespondentRow> respondents,
            ScaleReportExcelStyles styles
    ) {
        if (index < respondents.size()) {
            ScaleRespondentRow respondent = respondents.get(index);
            createCell(row, 0, String.valueOf(respondent.respondentNumber()), styles.data());
            createCell(row, 1, String.valueOf(respondent.responseValue()), styles.data());
            return;
        }
        createCell(row, 0, "", styles.data());
        createCell(row, 1, "", styles.data());
    }

    private void writeStatCells(Row row, ScaleValueStatRow stat, ScaleReportExcelStyles styles) {
        createCell(row, STAT_VALUE_COLUMN, String.valueOf(stat.responseValue()), styles.data());
        createCell(row, STAT_COUNT_COLUMN, String.valueOf(stat.responseCount()), styles.data());
        createCell(row, STAT_RATIO_COLUMN, stat.ratioPercent(), styles.data());
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
