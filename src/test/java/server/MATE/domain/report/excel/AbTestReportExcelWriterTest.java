package server.MATE.domain.report.excel;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class AbTestReportExcelWriterTest {

    private final AbTestReportExcelWriter writer = new AbTestReportExcelWriter();

    @Test
    void AB테스트_통계_템플릿_행이_생성된다() throws Exception {
        AbTestReportExcelData data = new AbTestReportExcelData(
                "Q01",
                "어떤 디자인이 더 좋나요?",
                5,
                3,
                2
        );

        byte[] bytes = writer.write(data);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("AB 테스트 통계");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Q01 - 질문 설정");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("A/B 테스트");
            assertThat(sheet.getRow(3).getCell(0).getStringCellValue()).isEqualTo("AB 테스트");
            assertThat(sheet.getRow(5).getCell(0).getStringCellValue()).isEqualTo("총 개수");
            assertThat(sheet.getRow(5).getCell(1).getStringCellValue()).isEqualTo("5");
            assertThat(sheet.getRow(6).getCell(0).getStringCellValue()).isEqualTo("Version A");
            assertThat(sheet.getRow(6).getCell(1).getStringCellValue()).isEqualTo("3");
            assertThat(sheet.getRow(7).getCell(0).getStringCellValue()).isEqualTo("Version B");
            assertThat(sheet.getRow(7).getCell(1).getStringCellValue()).isEqualTo("2");
        }
    }
}
