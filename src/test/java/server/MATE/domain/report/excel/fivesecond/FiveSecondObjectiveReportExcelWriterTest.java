package server.MATE.domain.report.excel.fivesecond;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FiveSecondObjectiveReportExcelWriterTest {

    private final FiveSecondObjectiveReportExcelWriter writer = new FiveSecondObjectiveReportExcelWriter();

    @Test
    void 객관식_5초_테스트_통계_템플릿_행이_생성된다() throws Exception {
        FiveSecondObjectiveReportExcelData data = new FiveSecondObjectiveReportExcelData(
                "Q01",
                "화면에서 가장 눈에 띄는 요소는?",
                List.of(
                        new FiveSecondRespondentRow(100L, "검색창"),
                        new FiveSecondRespondentRow(101L, "메인 배너")
                ),
                List.of(
                        new FiveSecondOptionStatRow("검색창", 3, "60"),
                        new FiveSecondOptionStatRow("메인 배너", 2, "40")
                ),
                5
        );

        byte[] bytes = writer.write(data);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("5초 테스트 통계");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("5초 테스트");
            assertThat(sheet.getRow(3).getCell(0).getStringCellValue()).isEqualTo("5초 테스트");
            assertThat(sheet.getRow(3).getCell(4).getStringCellValue()).isEqualTo("📊 5초 테스트 - 통계");
            assertThat(sheet.getRow(5).getCell(1).getStringCellValue()).isEqualTo("객관식");
            assertThat(sheet.getRow(5).getCell(4).getStringCellValue()).isEqualTo("응답 내용");
            assertThat(sheet.getRow(7).getCell(1).getStringCellValue()).isEqualTo("검색창");
            assertThat(sheet.getRow(7).getCell(4).getStringCellValue()).isEqualTo("검색창");
            assertThat(sheet.getRow(9).getCell(4).getStringCellValue()).isEqualTo("합계");
        }
    }
}
