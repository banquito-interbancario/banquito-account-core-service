package ec.edu.espe.banquito.accountcore.client;

import ec.edu.espe.banquito.accountcore.dto.AccountingEntryReqDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class AccountingServiceClient {

    private final WebClient webClient;

    public AccountingServiceClient(
            WebClient.Builder webClientBuilder,
            @Value("${accounting.service.url:http://accounting-service:8082}") String accountingUrl) {
        this.webClient = webClientBuilder.baseUrl(accountingUrl).build();
    }

    public void sendAccountingEntry(AccountingEntryReqDTO entry) {
        this.webClient.post()
                .uri("/api/v2/accounting/entries")
                .bodyValue(entry)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(new RuntimeException("Error en contabilidad: " + errorBody)))
                )
                .toBodilessEntity()
                // Timeout estricto de 5 segundos requerido por la especificación
                .timeout(Duration.ofSeconds(5))
                .block(); // Bloqueante porque el flujo del Core debe ser síncrono
    }
}