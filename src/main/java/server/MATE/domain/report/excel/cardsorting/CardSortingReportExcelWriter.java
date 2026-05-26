package server.MATE.domain.report.excel.cardsorting;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import server.MATE.domain.report.excel.common.ReportExcelWriteMode;

import static server.MATE.domain.report.excel.common.ReportExcelMergeSupport.mergeRow;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

@Component
public class CardSortingReportExcelWriter {

    private static final int LAST_COLUMN = 6;
    private static final int LEFT_LAST_COLUMN = 2;
    private static final int LEFT_COLUMN_WIDTH = 18 * 256;
    private static final int SHARED_COLUMN_WIDTH = 16 * 256;
    private static final int STAT_CATEGORY_COLUMN = 4;
    private static final int STAT_CARD_COLUMN = 5;
    private static final int STAT_RATIO_COLUMN = 6;

    private static final String SHEET_NAME = "카드소팅 통계";

    public byte[] write(CardSortingReportExcelData data) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            writeToSheet(sheet, 0, data);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("카드소팅 통계 엑셀 생성에 실패했습니다.", e);
        }
    }

    public int writeToSheet(Sheet sheet, int startRowIndex, CardSortingReportExcelData data) {
        return writeToSheet(sheet, startRowIndex, data, ReportExcelWriteMode.STANDALONE);
    }

    public int writeToSheet(
            Sheet sheet,
            int startRowIndex,
            CardSortingReportExcelData data,
            ReportExcelWriteMode mode
    ) {
        if (startRowIndex == 0 && mode == ReportExcelWriteMode.STANDALONE) {
            configureColumnWidths(sheet);
        }
        CardSortingReportExcelStyles styles = CardSortingReportExcelStyles.create(sheet.getWorkbook());

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
            CardSortingReportExcelStyles styles
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
            CardSortingReportExcelStyles styles
    ) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(22f);

        createCell(row, 0, "질문 유형", styles.label());
        createCell(row, 1, "카드소팅", styles.value());
        mergeRow(row, 1, LEFT_LAST_COLUMN, styles.value());
        createCell(row, STAT_CATEGORY_COLUMN, "질문 제목", styles.label());
        createCell(row, STAT_CARD_COLUMN, questionTitle == null ? "" : questionTitle, styles.value());
        mergeRow(row, STAT_CARD_COLUMN, LAST_COLUMN, styles.value());
        return rowIndex + 1;
    }

    private int writeSectionTitleRow(Sheet sheet, int rowIndex, CardSortingReportExcelStyles styles) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(24f);

        createCell(row, 0, "카드소팅", styles.sectionHeader());
        mergeRow(row, 0, LEFT_LAST_COLUMN, styles.sectionHeader());

        createCell(row, STAT_CATEGORY_COLUMN, "카드소팅 - 통계", styles.sectionHeader());
        mergeRow(row, STAT_CATEGORY_COLUMN, LAST_COLUMN, styles.sectionHeader());
        return rowIndex + 1;
    }

    private int writeQuestionAndStatHeaderRow(Sheet sheet, int rowIndex, CardSortingReportExcelStyles styles) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(22f);

        createCell(row, 0, "질문", styles.questionLabel());
        mergeRow(row, 0, LEFT_LAST_COLUMN, styles.questionLabel());

        createCell(row, STAT_CATEGORY_COLUMN, "카테고리명", styles.tableHeader());
        createCell(row, STAT_CARD_COLUMN, "카드", styles.tableHeader());
        createCell(row, STAT_RATIO_COLUMN, "비율 (%)", styles.tableHeader());
        return rowIndex + 1;
    }

    private int writeLeftTableHeaderRow(Sheet sheet, int rowIndex, CardSortingReportExcelStyles styles) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(22f);

        createCell(row, 0, "응답자 번호", styles.tableHeader());
        createCell(row, 1, "카테고리명", styles.tableHeader());
        createCell(row, 2, "카드번호", styles.tableHeader());
        return rowIndex + 1;
    }

    private int writeDataRows(
            Sheet sheet,
            int rowIndex,
            CardSortingReportExcelData data,
            CardSortingReportExcelStyles styles
    ) {
        List<CardSortingRespondentRow> respondents = data.respondents();
        List<CardSortingCategoryStatRow> categoryStats = data.categoryStats();
        int rowCount = Math.max(respondents.size(), categoryStats.size());

        for (int relativeRow = 0; relativeRow < rowCount; relativeRow++) {
            Row row = sheet.createRow(rowIndex + relativeRow);
            row.setHeightInPoints(22f);

            if (relativeRow < respondents.size()) {
                CardSortingRespondentRow respondent = respondents.get(relativeRow);
                createCell(row, 0, String.valueOf(respondent.respondentNumber()), styles.leftData());
                createCell(row, 1, respondent.categoryName(), styles.leftData());
                createCell(row, 2, String.valueOf(respondent.cardNumber()), styles.leftData());
            } else {
                createCell(row, 0, "", styles.leftData());
                createCell(row, 1, "", styles.leftData());
                createCell(row, 2, "", styles.leftData());
            }

            if (relativeRow < categoryStats.size()) {
                CardSortingCategoryStatRow stat = categoryStats.get(relativeRow);
                createCell(row, STAT_CARD_COLUMN, stat.cardLabel(), styles.statData());
                createCell(row, STAT_RATIO_COLUMN, stat.ratioPercent(), styles.statData());
            }
        }

        mergeCategoryNameCells(sheet, rowIndex, categoryStats, styles);
        return rowIndex + rowCount;
    }

    private void mergeCategoryNameCells(
            Sheet sheet,
            int startRowIndex,
            List<CardSortingCategoryStatRow> categoryStats,
            CardSortingReportExcelStyles styles
    ) {
        if (categoryStats.isEmpty()) {
            return;
        }

        int blockStart = 0;
        String currentCategory = categoryStats.get(0).categoryName();
        for (int index = 1; index <= categoryStats.size(); index++) {
            boolean isBlockEnd = index == categoryStats.size()
                    || !categoryStats.get(index).categoryName().equals(currentCategory);
            if (!isBlockEnd) {
                continue;
            }

            int firstRow = startRowIndex + blockStart;
            int lastRow = startRowIndex + index - 1;
            Row firstDataRow = sheet.getRow(firstRow);
            createCell(firstDataRow, STAT_CATEGORY_COLUMN, currentCategory, styles.statData());
            mergeColumn(sheet, firstRow, lastRow, STAT_CATEGORY_COLUMN, styles.statData());

            if (index < categoryStats.size()) {
                blockStart = index;
                currentCategory = categoryStats.get(index).categoryName();
            }
        }
    }

    private void createCell(Row row, int columnIndex, String value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void mergeColumn(Sheet sheet, int firstRow, int lastRow, int columnIndex, CellStyle style) {
        if (firstRow >= lastRow) {
            return;
        }
        sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, columnIndex, columnIndex));
        applyStyleToMergedRegion(sheet, firstRow, lastRow, columnIndex, columnIndex, style);
    }

    private void applyStyleToMergedRegion(
            Sheet sheet,
            int firstRow,
            int lastRow,
            int firstCol,
            int lastCol,
            CellStyle style
    ) {
        for (int rowIndex = firstRow; rowIndex <= lastRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                row = sheet.createRow(rowIndex);
            }
            for (int columnIndex = firstCol; columnIndex <= lastCol; columnIndex++) {
                Cell cell = row.getCell(columnIndex);
                if (cell == null) {
                    cell = row.createCell(columnIndex);
                }
                cell.setCellStyle(style);
            }
        }
    }
}
