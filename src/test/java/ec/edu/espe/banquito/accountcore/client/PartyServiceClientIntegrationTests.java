package ec.edu.espe.banquito.accountcore.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PartyServiceClientIntegrationTests {

    @Test
    @EnabledIfEnvironmentVariable(named = "RUN_PARTY_INTEGRATION", matches = "true")
    void validatesCustomerAndResolvesAccountHolder() {
        PartyServiceClient client = new PartyServiceClient("localhost", 9093);

        try {
            assertDoesNotThrow(() -> client.validateActiveCustomer(1L));
            assertEquals("María García", client.getHolderNameByAccount("2200000001"));
        } finally {
            client.shutdown();
        }
    }
}
