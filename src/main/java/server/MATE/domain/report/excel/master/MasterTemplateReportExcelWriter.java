package server.MATE.domain.report.excel.master;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

@Component
public class MasterTemplateReportExcelWriter {

    private static final String SHEET_NAME = "마스터 템플릿";
    private static final int LAST_COLUMN = 8;
    private static final byte[] HEADER_BLUE = {(byte) 68, (byte) 114, (byte) 196};

    public byte[] write() {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            configureSheet(sheet);
            writeGlobalHeader(sheet, 0);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("마스터 템플릿 엑셀 생성에 실패했습니다.", e);
        }
    }

    public void configureSheet(Sheet sheet) {
        sheet.setColumnWidth(0, 18 * 256);
        for (int columnIndex = 1; columnIndex <= LAST_COLUMN; columnIndex++) {
            sheet.setColumnWidth(columnIndex, 16 * 256);
        }
    }

    public int writeGlobalHeader(Sheet sheet, int startRowIndex) {
        Workbook workbook = sheet.getWorkbook();
        XSSFCellStyle headerStyle = createHeaderBarStyle(workbook);

        Row headerRow = sheet.createRow(startRowIndex);
        headerRow.setHeightInPoints(8f);
        Cell headerCell = headerRow.createCell(0);
        headerCell.setCellStyle(headerStyle);
        mergeRow(sheet, startRowIndex, 0, LAST_COLUMN, headerStyle);

        return startRowIndex + 2;
    }

    private XSSFCellStyle createHeaderBarStyle(Workbook workbook) {
        XSSFCellStyle style = (XSSFCellStyle) workbook.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(HEADER_BLUE, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        return style;
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
