package server.MATE.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @io.swagger.v3.oas.annotations.info.Info(
                title = "MATE API",
                description = "MATE 서버 API 문서",
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
