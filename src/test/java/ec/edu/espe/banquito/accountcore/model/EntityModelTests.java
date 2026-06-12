package ec.edu.espe.banquito.accountcore.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EntityModelTests {

    @Test
    void supportsEmptyAndIdConstructors() {
        LocalDate holidayDate = LocalDate.of(2026, Month.DECEMBER, 25);

        assertAll(
                () -> assertNotNull(new Account()),
                () -> assertEquals(1L, new Account(1L).getId()),
                () -> assertNotNull(new AccountSubtype()),
                () -> assertEquals(1, new AccountSubtype(1).getId()),
                () -> assertNotNull(new AccountTransaction()),
                () -> assertEquals(1L, new AccountTransaction(1L).getId()),
                () -> assertNotNull(new CoreParameter()),
                () -> assertEquals("CUT_OFF_TIME", new CoreParameter("CUT_OFF_TIME").getCode()),
                () -> assertNotNull(new CoreUser()),
                () -> assertEquals(1, new CoreUser(1).getId()),
                () -> assertNotNull(new Holiday()),
                () -> assertEquals(holidayDate, new Holiday(holidayDate).getHolidayDate()),
                () -> assertNotNull(new InstitutionalAccount()),
                () -> assertEquals(1, new InstitutionalAccount(1).getId()),
                () -> assertNotNull(new TransactionSubtype()),
                () -> assertEquals(1, new TransactionSubtype(1).getId())
        );
    }

    @Test
    void comparesEntitiesByPersistentIdentifier() {
        Account firstAccount = new Account(1L);
        Account secondAccount = new Account(1L);

        assertAll(
                () -> assertEquals(firstAccount, secondAccount),
                () -> assertEquals(firstAccount.hashCode(), secondAccount.hashCode()),
                () -> assertNotEquals(new Account(1L), new Account(2L)),
                () -> assertEquals(new AccountSubtype(1), new AccountSubtype(1)),
                () -> assertEquals(new AccountTransaction(1L), new AccountTransaction(1L)),
                () -> assertEquals(new CoreParameter("CUT_OFF_TIME"), new CoreParameter("CUT_OFF_TIME")),
                () -> assertEquals(new CoreUser(1), new CoreUser(1)),
                () -> assertEquals(
                        new Holiday(LocalDate.of(2026, Month.DECEMBER, 25)),
                        new Holiday(LocalDate.of(2026, Month.DECEMBER, 25))
                ),
                () -> assertEquals(new InstitutionalAccount(1), new InstitutionalAccount(1)),
                () -> assertEquals(new TransactionSubtype(1), new TransactionSubtype(1))
        );
    }

    @Test
    void producesSafeEntityDescriptions() {
        CoreUser user = new CoreUser(1);
        user.setUsername("cajero.norte");
        user.setPasswordHash("secret-hash");

        assertAll(
                () -> assertNotNull(new Account(1L).toString()),
                () -> assertNotNull(new AccountSubtype(1).toString()),
                () -> assertNotNull(new AccountTransaction(1L).toString()),
                () -> assertNotNull(new CoreParameter("CUT_OFF_TIME").toString()),
                () -> assertFalse(user.toString().contains("secret-hash")),
                () -> assertNotNull(new Holiday(LocalDate.of(2026, Month.DECEMBER, 25)).toString()),
                () -> assertNotNull(new InstitutionalAccount(1).toString()),
                () -> assertNotNull(new TransactionSubtype(1).toString())
        );
    }
}
