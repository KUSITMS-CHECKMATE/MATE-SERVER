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

@Component
public class AbTestReportExcelWriter {

    private static final int LAST_COLUMN = 6;
    private static final int CONTENT_LAST_COLUMN = 2;

    private static final String SHEET_NAME = "AB 테스트 통계";

    public byte[] write(AbTestReportExcelData data) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            writeToSheet(sheet, 0, data);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("AB 테스트 통계 엑셀 생성에 실패했습니다.", e);
        }
    }

    public int writeToSheet(Sheet sheet, int startRowIndex, AbTestReportExcelData data) {
        return writeToSheet(sheet, startRowIndex, data, ReportExcelWriteMode.STANDALONE);
    }

    public int writeToSheet(
            Sheet sheet,
            int startRowIndex,
            AbTestReportExcelData data,
            ReportExcelWriteMode mode
    ) {
        if (startRowIndex == 0 && mode == ReportExcelWriteMode.STANDALONE) {
            configureColumnWidths(sheet);
        }
        AbTestReportExcelStyles styles = AbTestReportExcelStyles.create(sheet.getWorkbook());

        int rowIndex = startRowIndex;
        if (mode == ReportExcelWriteMode.STANDALONE) {
            rowIndex = writeQuestionSettingHeader(sheet, rowIndex, data.questionNumberLabel(), styles);
            rowIndex = writeQuestionMetaRow(sheet, rowIndex, data.questionTitle(), styles);
            rowIndex++;
        }
        rowIndex = writeSectionTitleRow(sheet, rowIndex, styles);
        rowIndex = writeQuestionTextRow(sheet, rowIndex, data.questionTitle(), styles);
        rowIndex = writeTotalCountRow(sheet, rowIndex, data.totalCount(), styles);
        return writeVersionRows(sheet, rowIndex, data, styles);
    }

    private void configureColumnWidths(Sheet sheet) {
        sheet.setColumnWidth(0, 16 * 256);
        sheet.setColumnWidth(1, 20 * 256);
        sheet.setColumnWidth(2, 20 * 256);
        sheet.setColumnWidth(3, 12 * 256);
        sheet.setColumnWidth(4, 16 * 256);
        sheet.setColumnWidth(5, 16 * 256);
        sheet.setColumnWidth(6, 16 * 256);
    }

    private int writeQuestionSettingHeader(
            Sheet sheet,
            int rowIndex,
            String questionNumberLabel,
            AbTestReportExcelStyles styles
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
            AbTestReportExcelStyles styles
    ) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(22f);

        createCell(row, 0, "질문 유형", styles.label());
        createCell(row, 1, "A/B 테스트", styles.value());
        mergeRow(sheet, rowIndex, 1, CONTENT_LAST_COLUMN, styles.value());

        createCell(row, 4, "질문 제목", styles.label());
        createCell(row, 5, questionTitle == null ? "" : questionTitle, styles.value());
        mergeRow(sheet, rowIndex, 5, LAST_COLUMN, styles.value());
        return rowIndex + 1;
    }

    private int writeSectionTitleRow(Sheet sheet, int rowIndex, AbTestReportExcelStyles styles) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(24f);
        createCell(row, 0, "AB 테스트", styles.sectionHeader());
        mergeRow(sheet, rowIndex, 0, CONTENT_LAST_COLUMN, styles.sectionHeader());
        return rowIndex + 1;
    }

    private int writeQuestionTextRow(
            Sheet sheet,
            int rowIndex,
            String questionTitle,
            AbTestReportExcelStyles styles
    ) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(22f);

        createCell(row, 0, "질문", styles.greenLabel());
        createCell(row, 1, questionTitle == null ? "" : questionTitle, styles.greenValue());
        mergeRow(sheet, rowIndex, 1, CONTENT_LAST_COLUMN, styles.greenValue());
        return rowIndex + 1;
    }

    private int writeTotalCountRow(Sheet sheet, int rowIndex, int totalCount, AbTestReportExcelStyles styles) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(22f);

        createCell(row, 0, "총 개수", styles.greenLabel());
        createCell(row, 1, String.valueOf(totalCount), styles.greenValue());
        mergeRow(sheet, rowIndex, 1, CONTENT_LAST_COLUMN, styles.greenValue());
        return rowIndex + 1;
    }

    private int writeVersionRows(Sheet sheet, int rowIndex, AbTestReportExcelData data, AbTestReportExcelStyles styles) {
        Row versionARow = sheet.createRow(rowIndex);
        versionARow.setHeightInPoints(22f);
        createCell(versionARow, 0, "Version A", styles.versionLabel());
        createCell(versionARow, 1, String.valueOf(data.versionACount()), styles.countValue());
        mergeRow(sheet, rowIndex, 1, CONTENT_LAST_COLUMN, styles.countValue());

        Row versionBRow = sheet.createRow(rowIndex + 1);
        versionBRow.setHeightInPoints(22f);
        createCell(versionBRow, 0, "Version B", styles.versionLabel());
        createCell(versionBRow, 1, String.valueOf(data.versionBCount()), styles.countValue());
        mergeRow(sheet, rowIndex + 1, 1, CONTENT_LAST_COLUMN, styles.countValue());
        return rowIndex + 2;
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
