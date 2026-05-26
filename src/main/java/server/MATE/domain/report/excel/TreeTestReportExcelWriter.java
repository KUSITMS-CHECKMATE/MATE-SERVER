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
public class TreeTestReportExcelWriter {

    private static final int LAST_COLUMN = 8;
    private static final int LEFT_LAST_COLUMN = 2;
    private static final int STAT_PATH_FIRST_COLUMN = 4;
    private static final int STAT_PATH_LAST_COLUMN = 6;
    private static final int STAT_COUNT_COLUMN = 7;
    private static final int STAT_RATIO_COLUMN = 8;
    private static final int LEFT_COLUMN_WIDTH = 18 * 256;
    private static final int SHARED_COLUMN_WIDTH = 16 * 256;

    private static final String SHEET_NAME = "트리 테스트 통계";

    public byte[] write(TreeTestReportExcelData data) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            writeToSheet(sheet, 0, data);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("트리 테스트 통계 엑셀 생성에 실패했습니다.", e);
        }
    }

    public int writeToSheet(Sheet sheet, int startRowIndex, TreeTestReportExcelData data) {
        return writeToSheet(sheet, startRowIndex, data, ReportExcelWriteMode.STANDALONE);
    }

    public int writeToSheet(
            Sheet sheet,
            int startRowIndex,
            TreeTestReportExcelData data,
            ReportExcelWriteMode mode
    ) {
        if (startRowIndex == 0 && mode == ReportExcelWriteMode.STANDALONE) {
            configureColumnWidths(sheet);
        }
        TreeTestReportExcelStyles styles = TreeTestReportExcelStyles.create(sheet.getWorkbook());

        int rowIndex = startRowIndex;
        if (mode == ReportExcelWriteMode.STANDALONE) {
            rowIndex = writeQuestionSettingHeader(sheet, rowIndex, data.questionNumberLabel(), styles);
            rowIndex = writeQuestionMetaRow(sheet, rowIndex, data.questionTitle(), styles);
            rowIndex++;
        }
        rowIndex = writeSectionTitleRow(sheet, rowIndex, styles);
        rowIndex = writeQuestionAndStatHeaderRow(sheet, rowIndex, styles);
        rowIndex = writeLeftTableHeaderRow(sheet, rowIndex, styles);
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
            TreeTestReportExcelStyles styles
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
            TreeTestReportExcelStyles styles
    ) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(22f);

        createCell(row, 0, "질문 유형", styles.label());
        createCell(row, 1, "트리 테스트", styles.value());
        mergeRow(sheet, rowIndex, 1, LEFT_LAST_COLUMN, styles.value());
        createCell(row, STAT_PATH_FIRST_COLUMN, "질문 제목", styles.label());
        createCell(row, STAT_COUNT_COLUMN, questionTitle == null ? "" : questionTitle, styles.value());
        mergeRow(sheet, rowIndex, STAT_COUNT_COLUMN, LAST_COLUMN, styles.value());
        return rowIndex + 1;
    }

    private int writeSectionTitleRow(Sheet sheet, int rowIndex, TreeTestReportExcelStyles styles) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(24f);

        createCell(row, 0, "트리 테스트", styles.sectionHeader());
        mergeRow(sheet, rowIndex, 0, LEFT_LAST_COLUMN, styles.sectionHeader());

        createCell(row, STAT_PATH_FIRST_COLUMN, "📊 트리 테스트 - 통계", styles.sectionHeader());
        mergeRow(sheet, rowIndex, STAT_PATH_FIRST_COLUMN, LAST_COLUMN, styles.sectionHeader());
        return rowIndex + 1;
    }

    private int writeQuestionAndStatHeaderRow(Sheet sheet, int rowIndex, TreeTestReportExcelStyles styles) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(22f);

        createCell(row, 0, "질문", styles.questionLabel());
        mergeRow(sheet, rowIndex, 0, LEFT_LAST_COLUMN, styles.questionLabel());

        createCell(row, STAT_PATH_FIRST_COLUMN, "응답 경로", styles.tableHeader());
        mergeRow(sheet, rowIndex, STAT_PATH_FIRST_COLUMN, STAT_PATH_LAST_COLUMN, styles.tableHeader());
        createCell(row, STAT_COUNT_COLUMN, "응답 수", styles.tableHeader());
        createCell(row, STAT_RATIO_COLUMN, "비율(%)", styles.tableHeader());
        return rowIndex + 1;
    }

    private int writeLeftTableHeaderRow(Sheet sheet, int rowIndex, TreeTestReportExcelStyles styles) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(22f);

        createCell(row, 0, "응답자 번호", styles.tableHeader());
        createCell(row, 1, "응답", styles.tableHeader());
        createCell(row, 2, "Depth (숫자)", styles.tableHeader());
        return rowIndex + 1;
    }

    private int writeDataRows(
            Sheet sheet,
            int rowIndex,
            TreeTestReportExcelData data,
            TreeTestReportExcelStyles styles
    ) {
        List<TreeTestRespondentRow> respondents = data.respondents();
        List<TreeTestPathStatRow> pathStats = data.pathStats();
        int rowCount = Math.max(respondents.size(), pathStats.size() + 1);

        for (int relativeRow = 0; relativeRow < rowCount; relativeRow++) {
            Row row = sheet.createRow(rowIndex + relativeRow);
            row.setHeightInPoints(22f);
            writeRespondentCells(row, relativeRow, respondents, styles);
            writeStatCells(sheet, row, relativeRow, pathStats, data.totalResponseCount(), styles);
        }
        return rowIndex + rowCount;
    }

    private void writeRespondentCells(
            Row row,
            int index,
            List<TreeTestRespondentRow> respondents,
            TreeTestReportExcelStyles styles
    ) {
        if (index < respondents.size()) {
            TreeTestRespondentRow respondent = respondents.get(index);
            createCell(row, 0, String.valueOf(respondent.respondentNumber()), styles.data());
            createCell(row, 1, respondent.response(), styles.data());
            createCell(row, 2, String.valueOf(respondent.depth()), styles.data());
            return;
        }
        createCell(row, 0, "", styles.data());
        createCell(row, 1, "", styles.data());
        createCell(row, 2, "", styles.data());
    }

    private void writeStatCells(
            Sheet sheet,
            Row row,
            int index,
            List<TreeTestPathStatRow> pathStats,
            int totalResponseCount,
            TreeTestReportExcelStyles styles
    ) {
        if (index < pathStats.size()) {
            TreeTestPathStatRow stat = pathStats.get(index);
            createCell(row, STAT_PATH_FIRST_COLUMN, stat.pathLabel(), styles.pathData());
            mergeRow(sheet, row.getRowNum(), STAT_PATH_FIRST_COLUMN, STAT_PATH_LAST_COLUMN, styles.pathData());
            createCell(row, STAT_COUNT_COLUMN, String.valueOf(stat.responseCount()), styles.data());
            createCell(row, STAT_RATIO_COLUMN, stat.ratioPercent(), styles.data());
            return;
        }
        if (index == pathStats.size()) {
            createCell(row, STAT_PATH_FIRST_COLUMN, "합계", styles.totalLabel());
            mergeRow(sheet, row.getRowNum(), STAT_PATH_FIRST_COLUMN, STAT_PATH_LAST_COLUMN, styles.totalLabel());
            createCell(row, STAT_COUNT_COLUMN, String.valueOf(totalResponseCount), styles.data());
            createCell(row, STAT_RATIO_COLUMN, "-", styles.data());
            return;
        }
        createCell(row, STAT_PATH_FIRST_COLUMN, "", styles.pathData());
        mergeRow(sheet, row.getRowNum(), STAT_PATH_FIRST_COLUMN, STAT_PATH_LAST_COLUMN, styles.pathData());
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
