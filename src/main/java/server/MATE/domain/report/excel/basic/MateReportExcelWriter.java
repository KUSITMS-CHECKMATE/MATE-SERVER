package server.MATE.domain.report.excel.basic;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import server.MATE.domain.report.excel.common.QuestionTypeLabels;
import server.MATE.domain.question.dto.response.QuestionSummaryItem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

@Component
public class MateReportExcelWriter {

    public static final int MAX_QUESTION_ROWS = 20;
    private static final String SHEET_NAME = "기본 정보";

    public byte[] write(TestReportExcelData data) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            writeToSheet(sheet, 0, data);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("엑셀 보고서 생성에 실패했습니다.", e);
        }
    }

    public int writeToSheet(Sheet sheet, int startRowIndex, TestReportExcelData data) {
        if (startRowIndex == 0) {
            configureColumnWidths(sheet);
        }
        MateReportExcelStyles styles = MateReportExcelStyles.create(sheet.getWorkbook());

        int rowIndex = startRowIndex;
        rowIndex = writeSectionHeader(sheet, rowIndex, "테스트 기본정보", styles.sectionHeader());
        rowIndex = writeBasicInfoRow(sheet, rowIndex, "테스트명", data.testTitle(), styles);
        rowIndex = writeBasicInfoRow(sheet, rowIndex, "설명", data.testDescription(), styles);
        rowIndex = writeBasicInfoRow(sheet, rowIndex, "테스트 기간", data.testPeriod(), styles);
        rowIndex = writeBasicInfoRow(sheet, rowIndex, "대상 인원 명수", String.valueOf(data.targetParticipantCount()), styles);

        rowIndex++;
        rowIndex = writeSectionHeader(sheet, rowIndex, "질문 목록", styles.sectionHeader());
        rowIndex = writeQuestionHeaderRow(sheet, rowIndex, styles);

        List<QuestionSummaryItem> questions = data.questions();
        for (int index = 0; index < questions.size(); index++) {
            QuestionSummaryItem question = questions.get(index);
            writeQuestionRow(
                    sheet,
                    rowIndex + index,
                    index + 1,
                    QuestionTypeLabels.label(question.type()),
                    question.title(),
                    styles
            );
        }
        return rowIndex + questions.size();
    }

    private void configureColumnWidths(Sheet sheet) {
        sheet.setColumnWidth(0, 18 * 256);
        sheet.setColumnWidth(1, 24 * 256);
        sheet.setColumnWidth(2, 48 * 256);
    }

    private int writeSectionHeader(Sheet sheet, int rowIndex, String title, org.apache.poi.ss.usermodel.CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(24f);
        Cell cell = row.createCell(0);
        cell.setCellValue(title);
        cell.setCellStyle(style);
        mergeAndStyleRow(sheet, rowIndex, 0, 2, style);
        return rowIndex + 1;
    }

    private int writeBasicInfoRow(
            Sheet sheet,
            int rowIndex,
            String label,
            String value,
            MateReportExcelStyles styles
    ) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(22f);

        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(styles.label());

        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value == null ? "" : value);
        valueCell.setCellStyle(styles.value());

        Cell fillerCell = row.createCell(2);
        fillerCell.setCellStyle(styles.value());

        mergeAndStyleRow(sheet, rowIndex, 1, 2, styles.value());
        return rowIndex + 1;
    }

    private int writeQuestionHeaderRow(Sheet sheet, int rowIndex, MateReportExcelStyles styles) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(22f);

        createStyledCell(row, 0, "질문 번호", styles.questionSubHeader());
        createStyledCell(row, 1, "질문 템플릿 유형", styles.questionSubHeader());
        createStyledCell(row, 2, "질문 제목", styles.questionSubHeader());
        return rowIndex + 1;
    }

    private void writeQuestionRow(
            Sheet sheet,
            int rowIndex,
            int questionNumber,
            String questionTypeLabel,
            String questionTitle,
            MateReportExcelStyles styles
    ) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(22f);

        createStyledCell(row, 0, String.format("Q%02d", questionNumber), styles.questionNumber());
        createStyledCell(row, 1, questionTypeLabel, styles.questionData());
        createStyledCell(row, 2, questionTitle == null ? "" : questionTitle, styles.questionData());
    }

    private void createStyledCell(Row row, int columnIndex, String value, org.apache.poi.ss.usermodel.CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void mergeAndStyleRow(Sheet sheet, int rowIndex, int firstCol, int lastCol, org.apache.poi.ss.usermodel.CellStyle style) {
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
