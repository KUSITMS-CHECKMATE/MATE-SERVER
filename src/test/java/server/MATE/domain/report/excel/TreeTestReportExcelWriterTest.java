package server.MATE.domain.report.excel;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TreeTestReportExcelWriterTest {

    private final TreeTestReportExcelWriter writer = new TreeTestReportExcelWriter();

    @Test
    void 트리_테스트_통계_템플릿_행이_생성된다() throws Exception {
        TreeTestReportExcelData data = new TreeTestReportExcelData(
                "Q01",
                "고객센터를 찾아보세요.",
                List.of(
                        new TreeTestRespondentRow(100L, "고객센터", 3),
                        new TreeTestRespondentRow(101L, "FAQ", 3)
                ),
                List.of(
                        new TreeTestPathStatRow("홈 > 지원 > 고객센터", 4, "67"),
                        new TreeTestPathStatRow("홈 > 지원 > FAQ", 2, "33")
                ),
                6
        );

        byte[] bytes = writer.write(data);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("트리 테스트 통계");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Q01 - 질문 설정");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("트리 테스트");
            assertThat(sheet.getRow(3).getCell(0).getStringCellValue()).isEqualTo("트리 테스트");
            assertThat(sheet.getRow(3).getCell(4).getStringCellValue()).isEqualTo("📊 트리 테스트 - 통계");
            assertThat(sheet.getRow(4).getCell(0).getStringCellValue()).isEqualTo("질문");
            assertThat(sheet.getRow(4).getCell(4).getStringCellValue()).isEqualTo("응답 경로");
            assertThat(sheet.getRow(4).getCell(7).getStringCellValue()).isEqualTo("응답 수");
            assertThat(sheet.getRow(4).getCell(8).getStringCellValue()).isEqualTo("비율(%)");
            assertThat(sheet.getRow(5).getCell(0).getStringCellValue()).isEqualTo("응답자 번호");
            assertThat(sheet.getRow(5).getCell(2).getStringCellValue()).isEqualTo("Depth (숫자)");
            assertThat(sheet.getRow(6).getCell(1).getStringCellValue()).isEqualTo("고객센터");
            assertThat(sheet.getRow(6).getCell(2).getStringCellValue()).isEqualTo("3");
            assertThat(sheet.getRow(6).getCell(4).getStringCellValue()).isEqualTo("홈 > 지원 > 고객센터");
            assertThat(sheet.getRow(6).getCell(7).getStringCellValue()).isEqualTo("4");
            assertThat(sheet.getRow(8).getCell(4).getStringCellValue()).isEqualTo("합계");
            assertThat(sheet.getRow(8).getCell(7).getStringCellValue()).isEqualTo("6");
        }
    }
}
