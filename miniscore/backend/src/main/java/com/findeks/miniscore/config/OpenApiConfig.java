package com.findeks.miniscore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Swagger UI icin OpenAPI tanimi.
 *
 * "bearerAuth" semasini tanimlamamizin sebebi: Swagger arayuzunde sag ustteki
 * "Authorize" dugmesine access token'i bir kez yapistirinca, korumali uclari
 * (ör. /api/scores/my-score) UI uzerinden Authorization: Bearer ... ile deneyebiliyoruz.
 * Bu tanim olmadan Swagger'dan korumali uca istek atmak 401 doner.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI miniscoreOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Findeks MiniScore API")
                        .version("v1")
                        .description("JWT korumalı kredi skoru mock servisi."))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components().addSecuritySchemes(securitySchemeName,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
