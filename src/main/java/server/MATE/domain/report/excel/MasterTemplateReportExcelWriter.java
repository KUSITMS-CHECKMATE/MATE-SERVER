package server.MATE.domain.report.excel;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

@Component
public class MasterTemplateReportExcelWriter {

    private static final String SHEET_NAME = "마스터 템플릿";
    private static final String PLACEHOLDER_TEXT = "마스터 템플릿 준비 중입니다.";

    public byte[] write() {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            writeToSheet(sheet, 0);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("마스터 템플릿 엑셀 생성에 실패했습니다.", e);
        }
    }

    public int writeToSheet(Sheet sheet, int startRowIndex) {
        if (startRowIndex == 0) {
            configureColumnWidths(sheet);
        }
        CellStyle borderedStyle = createBorderedStyle(sheet.getWorkbook());

        Row row = sheet.createRow(startRowIndex);
        row.setHeightInPoints(22f);
        Cell cell = row.createCell(0);
        cell.setCellValue(PLACEHOLDER_TEXT);
        cell.setCellStyle(borderedStyle);
        return startRowIndex + 1;
    }

    private void configureColumnWidths(Sheet sheet) {
        sheet.setColumnWidth(0, 48 * 256);
    }

    private CellStyle createBorderedStyle(Workbook workbook) {
        XSSFCellStyle style = (XSSFCellStyle) workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.BLACK.getIndex());
        style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        style.setRightBorderColor(IndexedColors.BLACK.getIndex());
        return style;
    }
}
