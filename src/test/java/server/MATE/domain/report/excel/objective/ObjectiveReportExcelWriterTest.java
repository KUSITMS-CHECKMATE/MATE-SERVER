package server.MATE.domain.report.excel.objective;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ObjectiveReportExcelWriterTest {

    private final ObjectiveReportExcelWriter writer = new ObjectiveReportExcelWriter();

    @Test
    void 객관식_통계_템플릿_행이_생성된다() throws Exception {
        ObjectiveReportExcelData data = new ObjectiveReportExcelData(
                "Q01",
                "가장 자주 쓰는 기능은?",
                List.of(
                        new ObjectiveRespondentRow(1L, "선지 1"),
                        new ObjectiveRespondentRow(2L, "선지 2, 선지 3")
                ),
                List.of(
                        new ObjectiveOptionStatRow("선지 1", 1, "50"),
                        new ObjectiveOptionStatRow("선지 2", 1, "50")
                ),
                2
        );

        byte[] bytes = writer.write(data);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("객관식 통계");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Q01 - 질문 설정");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("객관식");
            assertThat(sheet.getRow(1).getCell(4).getStringCellValue()).isEqualTo("가장 자주 쓰는 기능은?");
            assertThat(sheet.getRow(3).getCell(0).getStringCellValue()).isEqualTo("객관식");
            assertThat(sheet.getRow(3).getCell(3).getStringCellValue()).isEqualTo("객관식 - 통계");
            assertThat(sheet.getRow(5).getCell(0).getStringCellValue()).isEqualTo("응답자 번호");
            assertThat(sheet.getRow(6).getCell(0).getStringCellValue()).isEqualTo("1");
            assertThat(sheet.getRow(6).getCell(3).getStringCellValue()).isEqualTo("선지 1");
            assertThat(sheet.getRow(8).getCell(3).getStringCellValue()).isEqualTo("합계");
            assertThat(sheet.getRow(8).getCell(4).getStringCellValue()).isEqualTo("2");
            assertThat(sheet.getRow(8).getCell(5).getStringCellValue()).isEqualTo("100");
        }
    }

    @Test
    void 응답이_없으면_비율은_대시로_표시한다() throws Exception {
        ObjectiveReportExcelData data = new ObjectiveReportExcelData(
                "Q02",
                "질문",
                List.of(),
                List.of(new ObjectiveOptionStatRow("선지 1", 0, "-")),
                0
        );

        byte[] bytes = writer.write(data);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(6).getCell(4).getStringCellValue()).isEqualTo("0");
            assertThat(sheet.getRow(6).getCell(5).getStringCellValue()).isEqualTo("-");
            assertThat(sheet.getRow(7).getCell(5).getStringCellValue()).isEqualTo("-");
        }
    }
}
