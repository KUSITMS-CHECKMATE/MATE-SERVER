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
public class FiveSecondSubjectiveReportExcelWriter {

    private static final int LAST_COLUMN = 6;
    private static final int LEFT_LAST_COLUMN = 2;
    private static final int LEFT_COLUMN_WIDTH = 18 * 256;
    private static final int SHARED_COLUMN_WIDTH = 16 * 256;

    public byte[] write(FiveSecondSubjectiveReportExcelData data) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("5초 테스트 통계");
            FiveSecondReportExcelStyles styles = FiveSecondReportExcelStyles.create(workbook);

            configureColumnWidths(sheet);

            int rowIndex = 0;
            rowIndex = writeQuestionSettingHeader(sheet, rowIndex, data.questionNumberLabel(), styles);
            rowIndex = writeQuestionMetaRow(sheet, rowIndex, data.questionTitle(), styles);
            rowIndex++;
            rowIndex = writeSectionTitleRow(sheet, rowIndex, styles);
            rowIndex = writeQuestionRow(sheet, rowIndex, data.questionTitle(), styles);
            rowIndex = writeQuestionTypeRow(sheet, rowIndex, styles);
            rowIndex = writeTableHeaderRow(sheet, rowIndex, styles);
            writeRespondentRows(sheet, rowIndex, data.respondents(), styles);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("5초 테스트(주관식) 통계 엑셀 생성에 실패했습니다.", e);
        }
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
        createCell(row, 4, "질문 제목", styles.label());
        createCell(row, 5, questionTitle == null ? "" : questionTitle, styles.value());
        mergeRow(sheet, rowIndex, 5, LAST_COLUMN, styles.value());
        return rowIndex + 1;
    }

    private int writeSectionTitleRow(Sheet sheet, int rowIndex, FiveSecondReportExcelStyles styles) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(24f);
        createCell(row, 0, "5초 테스트", styles.sectionHeader());
        mergeRow(sheet, rowIndex, 0, LEFT_LAST_COLUMN, styles.sectionHeader());
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

    private int writeQuestionTypeRow(Sheet sheet, int rowIndex, FiveSecondReportExcelStyles styles) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(22f);

        createCell(row, 0, "질문 유형 (객관/주관)", styles.questionLabel());
        createCell(row, 1, "주관식", styles.data());
        mergeRow(sheet, rowIndex, 1, LEFT_LAST_COLUMN, styles.data());
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

    private void writeRespondentRows(
            Sheet sheet,
            int rowIndex,
            List<FiveSecondRespondentRow> respondents,
            FiveSecondReportExcelStyles styles
    ) {
        for (int index = 0; index < respondents.size(); index++) {
            FiveSecondRespondentRow respondent = respondents.get(index);
            Row row = sheet.createRow(rowIndex + index);
            row.setHeightInPoints(22f);

            createCell(row, 0, String.valueOf(respondent.respondentNumber()), styles.data());
            createCell(row, 1, respondent.answerContent(), styles.data());
            mergeRow(sheet, rowIndex + index, 1, LEFT_LAST_COLUMN, styles.data());
        }
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
