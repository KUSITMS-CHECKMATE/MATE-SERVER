package server.MATE;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import server.MATE.global.storage.service.FileStorageService;

@SpringBootTest
@ActiveProfiles("test")
class MateApplicationTests {

	@MockitoBean
	private FileStorageService fileStorageService;

	@Test
	void contextLoads() {
	}

}
