package ec.edu.espe.banquito.accountcore.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:5174,http://localhost:3001,http://localhost:3002,http://localhost:3003,http://localhost:3004}")
    private String allowedOriginsRaw;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                String[] origins = allowedOriginsRaw.split(",");
                registry.addMapping("/api/v2/**")
                        .allowedOrigins(origins)
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }
}
