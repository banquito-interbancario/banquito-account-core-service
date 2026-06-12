package ec.edu.espe.banquito.accountcore.client;

import ec.edu.espe.banquito.accountcore.dto.AccountingEntryReqDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class AccountingServiceClientIntegrationTests {

    @Test
    @EnabledIfEnvironmentVariable(named = "RUN_ACCOUNTING_INTEGRATION", matches = "true")
    void registersBalancedEntryInAccountingService() {
        String entryUuid = System.getProperty("test.entry.uuid");
        AccountingServiceClient client = new AccountingServiceClient("localhost", 9092);

        try {
            assertDoesNotThrow(() -> client.registerEntry(new AccountingEntryReqDTO(
                            entryUuid,
                            "Integration test from account-core-service",
                            LocalDate.of(2026, Month.JUNE, 11),
                            List.of(
                                    new AccountingEntryReqDTO.JournalLineDTO(
                                            "1.1.0.02", "DEBITO", new BigDecimal("10.00"), entryUuid),
                                    new AccountingEntryReqDTO.JournalLineDTO(
                                            "2.1.0.01", "CREDITO", new BigDecimal("10.00"), entryUuid)
                            )
                    ))
            );
        } finally {
            client.shutdown();
        }
    }
}
