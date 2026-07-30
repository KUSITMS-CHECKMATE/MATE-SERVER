package server.MATE.global.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import server.MATE.global.common.exception.GlobalExceptionHandler;
import server.MATE.global.discord.channel.ErrorAlertChannel;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private ErrorAlertChannel errorAlertChannel;

    @InjectMocks
    private FileController fileController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(fileController)
                .setControllerAdvice(new GlobalExceptionHandler(errorAlertChannel))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    @DisplayName("media/ 접두사가 아닌 fileKey는 다운로드 URL 발급을 거부한다")
    void rejectsDownloadUrlForNonMediaKey() throws Exception {
        mockMvc.perform(get("/api/v1/files/presigned-url/download")
                        .queryParam("fileKey", "reports/pdf/1.pdf"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FILE_004"));

        verify(fileStorageService, never()).generateDownloadUrl("reports/pdf/1.pdf");
    }

    @Test
    @DisplayName("타인의 테스트 리포트 키를 추측해 요청해도 다운로드 URL 발급을 거부한다")
    void rejectsDownloadUrlForGuessedReportKey() throws Exception {
        mockMvc.perform(get("/api/v1/files/presigned-url/download")
                        .queryParam("fileKey", "reports/excel/999.xlsx"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FILE_004"));

        verify(fileStorageService, never()).generateDownloadUrl("reports/excel/999.xlsx");
    }

    @Test
    @DisplayName("media/ 접두사 fileKey는 정상적으로 다운로드 URL을 발급한다")
    void generatesDownloadUrlForMediaKey() throws Exception {
        given(fileStorageService.generateDownloadUrl("media/uuid.jpg")).willReturn("https://example.com/signed-url");

        mockMvc.perform(get("/api/v1/files/presigned-url/download")
                        .queryParam("fileKey", "media/uuid.jpg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.presignedUrl").value("https://example.com/signed-url"));
    }
}
