package fr.aplose.erp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AploseERP API")
                        .version("1.0")
                        .description("REST API for tiers, contacts, proposals, orders and invoices. Authenticate with HTTP Basic (user/password) or API Key: header X-API-Key or Authorization: Bearer <key>. Create keys in Admin → API Keys.")
                        .contact(new Contact().name("Aplose")))
                .servers(List.of(
                        new Server().url(baseUrl).description("Current server")
                ));
    }
}
