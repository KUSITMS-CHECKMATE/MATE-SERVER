package server.MATE.domain.report.excel.objective;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import server.MATE.domain.report.excel.common.ReportExcelWriteMode;

import static server.MATE.domain.report.excel.common.ReportExcelMergeSupport.mergeRow;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

@Component
public class ObjectiveReportExcelWriter {

    private static final int LAST_COLUMN = 5;

    private static final String SHEET_NAME = "객관식 통계";

    public byte[] write(ObjectiveReportExcelData data) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            writeToSheet(sheet, 0, data);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("객관식 통계 엑셀 생성에 실패했습니다.", e);
        }
    }

    public int writeToSheet(Sheet sheet, int startRowIndex, ObjectiveReportExcelData data) {
        return writeToSheet(sheet, startRowIndex, data, ReportExcelWriteMode.STANDALONE);
    }

    public int writeToSheet(
            Sheet sheet,
            int startRowIndex,
            ObjectiveReportExcelData data,
            ReportExcelWriteMode mode
    ) {
        if (startRowIndex == 0 && mode == ReportExcelWriteMode.STANDALONE) {
            configureColumnWidths(sheet);
        }
        ObjectiveReportExcelStyles styles = ObjectiveReportExcelStyles.create(sheet.getWorkbook());

        int rowIndex = startRowIndex;
        if (mode == ReportExcelWriteMode.STANDALONE) {
            rowIndex = writeQuestionSettingHeader(sheet, rowIndex, data.questionNumberLabel(), styles);
            rowIndex = writeQuestionMetaRow(sheet, rowIndex, data.questionTitle(), styles);
            rowIndex++;
        }
        rowIndex = writeSectionTitleRow(sheet, rowIndex, styles);
        rowIndex = writeQuestionSubLabelRow(sheet, rowIndex, styles);
        rowIndex = writeTableHeaderRow(sheet, rowIndex, styles);
        return writeDataRows(sheet, rowIndex, data, styles);
    }

    private void configureColumnWidths(Sheet sheet) {
        sheet.setColumnWidth(0, 16 * 256);
        sheet.setColumnWidth(1, 24 * 256);
        sheet.setColumnWidth(2, 4 * 256);
        sheet.setColumnWidth(3, 18 * 256);
        sheet.setColumnWidth(4, 12 * 256);
        sheet.setColumnWidth(5, 12 * 256);
    }

    private int writeQuestionSettingHeader(
            Sheet sheet,
            int rowIndex,
            String questionNumberLabel,
            ObjectiveReportExcelStyles styles
    ) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(24f);
        createCell(row, 0, questionNumberLabel + " - 질문 설정", styles.questionSettingHeader());
        mergeRow(row, 0, LAST_COLUMN, styles.questionSettingHeader());
        return rowIndex + 1;
    }

    private int writeQuestionMetaRow(
            Sheet sheet,
            int rowIndex,
            String questionTitle,
            ObjectiveReportExcelStyles styles
    ) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(22f);

        createCell(row, 0, "질문 유형", styles.label());
        createCell(row, 1, "객관식", styles.value());
        createCell(row, 3, "질문 제목", styles.label());
        createCell(row, 4, questionTitle == null ? "" : questionTitle, styles.value());
        mergeRow(row, 4, LAST_COLUMN, styles.value());
        return rowIndex + 1;
    }

    private int writeSectionTitleRow(Sheet sheet, int rowIndex, ObjectiveReportExcelStyles styles) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(24f);

        createCell(row, 0, "객관식", styles.redSectionHeader());
        mergeRow(row, 0, 1, styles.redSectionHeader());

        createCell(row, 3, "객관식 - 통계", styles.redSectionHeader());
        mergeRow(row, 3, LAST_COLUMN, styles.redSectionHeader());
        return rowIndex + 1;
    }

    private int writeQuestionSubLabelRow(Sheet sheet, int rowIndex, ObjectiveReportExcelStyles styles) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(22f);
        createCell(row, 0, "질문", styles.greenSubLabel());
        return rowIndex + 1;
    }

    private int writeTableHeaderRow(Sheet sheet, int rowIndex, ObjectiveReportExcelStyles styles) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(22f);

        createCell(row, 0, "응답자 번호", styles.tableHeader());
        createCell(row, 1, "선택 선지", styles.tableHeader());
        createCell(row, 3, "선택 선지", styles.tableHeader());
        createCell(row, 4, "응답 수", styles.tableHeader());
        createCell(row, 5, "비율 (%)", styles.tableHeader());
        return rowIndex + 1;
    }

    private int writeDataRows(
            Sheet sheet,
            int rowIndex,
            ObjectiveReportExcelData data,
            ObjectiveReportExcelStyles styles
    ) {
        List<ObjectiveRespondentRow> respondents = data.respondents();
        List<ObjectiveOptionStatRow> optionStats = data.optionStats();
        int rowCount = Math.max(respondents.size(), optionStats.size());

        for (int index = 0; index < rowCount; index++) {
            Row row = sheet.createRow(rowIndex + index);
            row.setHeightInPoints(22f);

            if (index < respondents.size()) {
                ObjectiveRespondentRow respondent = respondents.get(index);
                createCell(row, 0, String.valueOf(respondent.respondentNumber()), styles.data());
                createCell(row, 1, respondent.selectedOption(), styles.data());
            } else {
                createCell(row, 0, "", styles.data());
                createCell(row, 1, "", styles.data());
            }

            if (index < optionStats.size()) {
                ObjectiveOptionStatRow stat = optionStats.get(index);
                createCell(row, 3, stat.optionLabel(), styles.data());
                createCell(row, 4, String.valueOf(stat.responseCount()), styles.data());
                createCell(row, 5, stat.ratioPercent(), styles.data());
            }
        }

        rowIndex += rowCount;
        Row totalRow = sheet.createRow(rowIndex);
        totalRow.setHeightInPoints(22f);
        createCell(totalRow, 3, "합계", styles.totalLabel());
        createCell(totalRow, 4, String.valueOf(data.totalResponses()), styles.data());
        createCell(totalRow, 5, data.totalResponses() == 0 ? "-" : "100", styles.data());
        return rowIndex + 1;
    }

    private void createCell(Row row, int columnIndex, String value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

}
