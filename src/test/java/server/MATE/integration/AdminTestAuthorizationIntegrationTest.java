package server.MATE.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import server.MATE.domain.auth.jwt.JwtProvider;
import server.MATE.domain.auth.jwt.TokenType;
import server.MATE.domain.users.entity.Role;
import server.MATE.domain.users.entity.Users;
import server.MATE.domain.users.repository.UsersRepository;
import server.MATE.global.storage.FileStorageService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminTestAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    private UsersRepository usersRepository;

    @MockitoBean
    private FileStorageService fileStorageService;

    @Test
    void 일반_유저는_관리자_테스트_API에_403을_받는다() throws Exception {
        Users user = usersRepository.save(Users.builder()
                .ci("user-" + System.nanoTime())
                .name("user")
                .role(Role.USER)
                .build());
        String token = "Bearer " + jwtProvider.generateToken(user.getId(), Role.USER.name(), TokenType.ACCESS);

        mockMvc.perform(get("/api/v1/admin/tests").header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    @Test
    void 관리자는_관리자_테스트_API를_호출할_수_있다() throws Exception {
        Users admin = usersRepository.save(Users.builder()
                .ci("admin-" + System.nanoTime())
                .name("admin")
                .role(Role.ADMIN)
                .build());
        String token = "Bearer " + jwtProvider.generateToken(admin.getId(), Role.ADMIN.name(), TokenType.ACCESS);

        mockMvc.perform(get("/api/v1/admin/tests").header("Authorization", token))
                .andExpect(status().isOk());
    }
}
