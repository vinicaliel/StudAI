package studAI.AI.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "API do StudAI", version = "1.0", description = "Backend do SaaS de Resumos"),
        security = {@SecurityRequirement(name = "bearerAuth")} // Aplica o cadeado em todas as rotas por padrão
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class SwaggerConfig {
    // A classe pode ficar vazia, as anotações mágicas do Spring cuidam de tudo!
}
