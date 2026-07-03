package ec.edu.espe.banquito.accountcore.service;

import ec.edu.espe.banquito.accountcore.dto.OffUsPaymentMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class ClearingPublisher {

    private static final ZoneId BANK_ZONE = ZoneId.of("America/Guayaquil");

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String clearingTopic;

    public ClearingPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                             @Value("${app.kafka.clearing-topic:clearing-outbound}") String clearingTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.clearingTopic = clearingTopic;
    }

    public void publishExternalTransfer(String originAccountNumber, String externalBankCode,
                                        String externalAccountNumber, BigDecimal amount,
                                        String concept) {
        OffUsPaymentMessage message = new OffUsPaymentMessage();
        message.setBatchId(UUID.randomUUID());
        message.setTransactionId(UUID.randomUUID());
        message.setRoutingCode(externalBankCode);
        message.setOriginAccount(originAccountNumber);
        message.setDestinationAccount(externalAccountNumber);
        message.setAmount(amount);
        message.setCurrency("USD");
        message.setConcept(concept);
        message.setValueDate(LocalDate.now(BANK_ZONE));

        kafkaTemplate.send(clearingTopic, message.getBatchId().toString(), message);
    }
}
