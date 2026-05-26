package server.MATE.domain.report.excel.common;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;

public final class ReportExcelMergeSupport {

    private ReportExcelMergeSupport() {
    }

    public static void mergeRow(Row row, int firstCol, int lastCol, CellStyle style) {
        if (firstCol == lastCol) {
            return;
        }
        int rowIndex = row.getRowNum();
        row.getSheet().addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, firstCol, lastCol));
        for (int columnIndex = firstCol + 1; columnIndex <= lastCol; columnIndex++) {
            Cell cell = row.getCell(columnIndex);
            if (cell == null) {
                cell = row.createCell(columnIndex);
            }
            cell.setCellStyle(style);
        }
    }
}
