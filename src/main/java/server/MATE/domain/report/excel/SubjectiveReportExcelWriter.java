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
public class SubjectiveReportExcelWriter {

    private static final int LAST_COLUMN = 6;
    private static final int CONTENT_LAST_COLUMN = 3;

    public byte[] write(SubjectiveReportExcelData data) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("주관식 통계");
            SubjectiveReportExcelStyles styles = SubjectiveReportExcelStyles.create(workbook);

            configureColumnWidths(sheet);

            int rowIndex = 0;
            rowIndex = writeQuestionSettingHeader(sheet, rowIndex, data.questionNumberLabel(), styles);
            rowIndex = writeQuestionMetaRow(sheet, rowIndex, data.questionTitle(), styles);
            rowIndex++;
            rowIndex = writeSectionTitleRow(sheet, rowIndex, styles);
            rowIndex = writeQuestionTextRow(sheet, rowIndex, data.questionTitle(), styles);
            rowIndex = writeTableHeaderRow(sheet, rowIndex, styles);
            writeRespondentRows(sheet, rowIndex, data.respondents(), styles);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("주관식 통계 엑셀 생성에 실패했습니다.", e);
        }
    }

    private void configureColumnWidths(Sheet sheet) {
        sheet.setColumnWidth(0, 16 * 256);
        sheet.setColumnWidth(1, 20 * 256);
        sheet.setColumnWidth(2, 20 * 256);
        sheet.setColumnWidth(3, 20 * 256);
        sheet.setColumnWidth(4, 16 * 256);
        sheet.setColumnWidth(5, 16 * 256);
        sheet.setColumnWidth(6, 16 * 256);
    }

    private int writeQuestionSettingHeader(
            Sheet sheet,
            int rowIndex,
            String questionNumberLabel,
            SubjectiveReportExcelStyles styles
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
            SubjectiveReportExcelStyles styles
    ) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(22f);

        createCell(row, 0, "질문 유형", styles.label());
        createCell(row, 1, "주관식", styles.value());
        mergeRow(sheet, rowIndex, 1, 3, styles.value());

        createCell(row, 4, "질문 제목", styles.label());
        createCell(row, 5, questionTitle == null ? "" : questionTitle, styles.value());
        mergeRow(sheet, rowIndex, 5, LAST_COLUMN, styles.value());
        return rowIndex + 1;
    }

    private int writeSectionTitleRow(Sheet sheet, int rowIndex, SubjectiveReportExcelStyles styles) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(24f);
        createCell(row, 0, "주관식", styles.sectionHeader());
        mergeRow(sheet, rowIndex, 0, CONTENT_LAST_COLUMN, styles.sectionHeader());
        return rowIndex + 1;
    }

    private int writeQuestionTextRow(
            Sheet sheet,
            int rowIndex,
            String questionTitle,
            SubjectiveReportExcelStyles styles
    ) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(22f);

        createCell(row, 0, "질문", styles.questionLabel());
        createCell(row, 1, questionTitle == null ? "" : questionTitle, styles.questionText());
        mergeRow(sheet, rowIndex, 1, CONTENT_LAST_COLUMN, styles.questionText());
        return rowIndex + 1;
    }

    private int writeTableHeaderRow(Sheet sheet, int rowIndex, SubjectiveReportExcelStyles styles) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(22f);

        createCell(row, 0, "응답자 번호", styles.tableHeader());
        createCell(row, 1, "답변 내용", styles.tableHeader());
        mergeRow(sheet, rowIndex, 1, CONTENT_LAST_COLUMN, styles.tableHeader());
        return rowIndex + 1;
    }

    private void writeRespondentRows(
            Sheet sheet,
            int rowIndex,
            List<SubjectiveRespondentRow> respondents,
            SubjectiveReportExcelStyles styles
    ) {
        for (int index = 0; index < respondents.size(); index++) {
            SubjectiveRespondentRow respondent = respondents.get(index);
            Row row = sheet.createRow(rowIndex + index);
            row.setHeightInPoints(22f);

            createCell(row, 0, String.valueOf(respondent.respondentNumber()), styles.data());
            createCell(row, 1, respondent.answerContent(), styles.data());
            mergeRow(sheet, rowIndex + index, 1, CONTENT_LAST_COLUMN, styles.data());
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
