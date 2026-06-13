package ec.edu.espe.banquito.accountcore.client;

import ec.edu.espe.banquito.accountcore.dto.AccountingOperationReqDTO;
import ec.edu.espe.banquito.accountcore.enums.AccountingOperationType;
import ec.edu.espe.banquito.accountcore.enums.AccountingProductType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class AccountingServiceClientIntegrationTests {

    @Test
    @EnabledIfEnvironmentVariable(named = "RUN_ACCOUNTING_INTEGRATION", matches = "true")
    void postsFunctionalOperationInAccountingService() {
        String entryUuid = System.getProperty("test.entry.uuid");
        AccountingServiceClient client = new AccountingServiceClient("localhost", 9092);

        try {
            assertDoesNotThrow(() -> client.postOperation(new AccountingOperationReqDTO(
                            entryUuid,
                            AccountingOperationType.TELLER_DEPOSIT,
                            AccountingProductType.SAVINGS,
                            new BigDecimal("10.00"),
                            null,
                            "Integration test from account-core-service",
                            LocalDate.of(2026, Month.JUNE, 11)
                    ))
            );
        } finally {
            client.shutdown();
        }
    }
}
