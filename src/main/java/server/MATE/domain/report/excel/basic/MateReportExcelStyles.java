package server.MATE.domain.report.excel.basic;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

record MateReportExcelStyles(
        CellStyle sectionHeader,
        CellStyle label,
        CellStyle value,
        CellStyle questionSubHeader,
        CellStyle questionNumber,
        CellStyle questionData
) {

    private static final byte[] HEADER_BLUE = {(byte) 68, (byte) 114, (byte) 196};
    private static final byte[] LABEL_BLUE = {(byte) 217, (byte) 225, (byte) 242};
    private static final byte[] VALUE_YELLOW = {(byte) 255, (byte) 242, (byte) 204};

    static MateReportExcelStyles create(Workbook workbook) {
        return new MateReportExcelStyles(
                createSectionHeaderStyle(workbook),
                createLabelStyle(workbook),
                createValueStyle(workbook),
                createQuestionSubHeaderStyle(workbook),
                createQuestionNumberStyle(workbook),
                createQuestionDataStyle(workbook)
        );
    }

    private static XSSFCellStyle createSectionHeaderStyle(Workbook workbook) {
        XSSFCellStyle style = coloredStyle(workbook, HEADER_BLUE);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        return style;
    }

    private static XSSFCellStyle createLabelStyle(Workbook workbook) {
        XSSFCellStyle style = coloredStyle(workbook, LABEL_BLUE);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private static XSSFCellStyle createValueStyle(Workbook workbook) {
        XSSFCellStyle style = coloredStyle(workbook, VALUE_YELLOW);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }

    private static XSSFCellStyle createQuestionSubHeaderStyle(Workbook workbook) {
        return createLabelStyle(workbook);
    }

    private static XSSFCellStyle createQuestionNumberStyle(Workbook workbook) {
        XSSFCellStyle style = borderedStyle(workbook);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private static XSSFCellStyle createQuestionDataStyle(Workbook workbook) {
        return createValueStyle(workbook);
    }

    private static XSSFCellStyle coloredStyle(Workbook workbook, byte[] rgb) {
        XSSFCellStyle style = borderedStyle(workbook);
        style.setFillForegroundColor(new XSSFColor(rgb, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private static XSSFCellStyle borderedStyle(Workbook workbook) {
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
