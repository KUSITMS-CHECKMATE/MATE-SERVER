package server.MATE.domain.report.excel;

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

record AbTestReportExcelStyles(
        CellStyle questionSettingHeader,
        CellStyle label,
        CellStyle value,
        CellStyle sectionHeader,
        CellStyle greenLabel,
        CellStyle greenValue,
        CellStyle versionLabel,
        CellStyle countValue
) {

    private static final byte[] HEADER_BLUE = {(byte) 68, (byte) 114, (byte) 196};
    private static final byte[] LABEL_BLUE = {(byte) 217, (byte) 225, (byte) 242};
    private static final byte[] VALUE_YELLOW = {(byte) 255, (byte) 242, (byte) 204};
    private static final byte[] SECTION_PURPLE = {(byte) 128, (byte) 87, (byte) 185};
    private static final byte[] LABEL_GREEN = {(byte) 198, (byte) 239, (byte) 206};

    static AbTestReportExcelStyles create(Workbook workbook) {
        return new AbTestReportExcelStyles(
                createQuestionSettingHeaderStyle(workbook),
                createLabelStyle(workbook),
                createPlainValueStyle(workbook),
                createSectionHeaderStyle(workbook),
                createGreenLabelStyle(workbook),
                createGreenValueStyle(workbook),
                createVersionLabelStyle(workbook),
                createCountValueStyle(workbook)
        );
    }

    private static XSSFCellStyle createQuestionSettingHeaderStyle(Workbook workbook) {
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
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private static XSSFCellStyle createPlainValueStyle(Workbook workbook) {
        XSSFCellStyle style = borderedStyle(workbook);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }

    private static XSSFCellStyle createSectionHeaderStyle(Workbook workbook) {
        XSSFCellStyle style = coloredStyle(workbook, SECTION_PURPLE);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        return style;
    }

    private static XSSFCellStyle createGreenLabelStyle(Workbook workbook) {
        XSSFCellStyle style = coloredStyle(workbook, LABEL_GREEN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private static XSSFCellStyle createGreenValueStyle(Workbook workbook) {
        XSSFCellStyle style = coloredStyle(workbook, LABEL_GREEN);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private static XSSFCellStyle createVersionLabelStyle(Workbook workbook) {
        return createLabelStyle(workbook);
    }

    private static XSSFCellStyle createCountValueStyle(Workbook workbook) {
        XSSFCellStyle style = coloredStyle(workbook, VALUE_YELLOW);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
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
