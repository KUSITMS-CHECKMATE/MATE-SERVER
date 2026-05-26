package server.MATE.domain.report.excel;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FiveSecondSubjectiveReportExcelWriterTest {

    private final FiveSecondSubjectiveReportExcelWriter writer = new FiveSecondSubjectiveReportExcelWriter();

    @Test
    void 주관식_5초_테스트_템플릿_행이_생성된다() throws Exception {
        FiveSecondSubjectiveReportExcelData data = new FiveSecondSubjectiveReportExcelData(
                "Q01",
                "5초간 본 화면을 설명해주세요.",
                List.of(
                        new FiveSecondRespondentRow(100L, "로고가 크게 보였어요"),
                        new FiveSecondRespondentRow(101L, "버튼 색상이 눈에 띄었습니다")
                )
        );

        byte[] bytes = writer.write(data);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("5초 테스트 통계");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("5초 테스트");
            assertThat(sheet.getRow(3).getCell(0).getStringCellValue()).isEqualTo("5초 테스트");
            assertThat(sheet.getRow(5).getCell(1).getStringCellValue()).isEqualTo("주관식");
            assertThat(sheet.getRow(6).getCell(1).getStringCellValue()).isEqualTo("응답 내용");
            assertThat(sheet.getRow(7).getCell(1).getStringCellValue()).isEqualTo("로고가 크게 보였어요");
            assertThat(sheet.getRow(3).getCell(4)).isNull();
        }
    }
}
