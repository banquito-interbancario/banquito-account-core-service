package ec.edu.espe.banquito.accountcore.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI accountCoreOpenAPI(@Value("${api.public.base-url:/}") String publicBaseUrl) {
        return new OpenAPI()
                .info(new Info()
                        .title("BanQuito Account Core API")
                        .version("2.0")
                        .description("REST API exposed by Oscar for account balances, transactions and payment-switch integration.")
                        .contact(new Contact()
                                .name("BanQuito Account Core Team")))
                .addServersItem(new Server()
                        .url(publicBaseUrl)
                        .description("Public API Manager or local base URL"));
    }
}
