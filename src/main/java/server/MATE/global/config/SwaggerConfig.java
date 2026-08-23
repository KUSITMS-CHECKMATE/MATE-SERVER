package server.MATE.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration;


@Configuration
@OpenAPIDefinition(
        info = @io.swagger.v3.oas.annotations.info.Info(
                title = "MATE API",
                description = """
                        ### MATE 서버 API 문서
                        
                        API 앞의 이모지를 통해 문서화 여부를 표시합니다.

                        - ✅ : 프론트가 사용할 API. 꼼꼼한 문서화 완료
                        - 📝 : 프론트가 사용할 API. 문서화 보충 필요
                        - 🙅🏻‍♀️ : 프론트가 연동할 필요 없음. 디버깅용으로 사용
                        - 🔒 : 프론트가 연동할 필요 없음. 관리자용 API
                        - ❓ : 프론트의 연동 필요 여부 불확실
                        """,
                version = "v1.0.0"
        ),
        security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "JWT")
)
@io.swagger.v3.oas.annotations.security.SecurityScheme(
        name = "JWT",
        description = "AccessToken를 입력해주세요.",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class SwaggerConfig {
}
