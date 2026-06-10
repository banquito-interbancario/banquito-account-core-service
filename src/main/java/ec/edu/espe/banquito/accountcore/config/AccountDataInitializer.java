package ec.edu.espe.banquito.accountcore.config;

import ec.edu.espe.banquito.accountcore.enums.AccountStatus;
import ec.edu.espe.banquito.accountcore.enums.AccountSuperType;
import ec.edu.espe.banquito.accountcore.enums.CatalogStatus;
import ec.edu.espe.banquito.accountcore.model.Account;
import ec.edu.espe.banquito.accountcore.model.AccountSubtype;
import ec.edu.espe.banquito.accountcore.model.CoreParameter;
import ec.edu.espe.banquito.accountcore.model.CoreUser;
import ec.edu.espe.banquito.accountcore.model.Holiday;
import ec.edu.espe.banquito.accountcore.model.InstitutionalAccount;
import ec.edu.espe.banquito.accountcore.model.TransactionSubtype;
import ec.edu.espe.banquito.accountcore.repository.AccountRepository;
import ec.edu.espe.banquito.accountcore.repository.AccountSubtypeRepository;
import ec.edu.espe.banquito.accountcore.repository.CoreParameterRepository;
import ec.edu.espe.banquito.accountcore.repository.CoreUserRepository;
import ec.edu.espe.banquito.accountcore.repository.HolidayRepository;
import ec.edu.espe.banquito.accountcore.repository.InstitutionalAccountRepository;
import ec.edu.espe.banquito.accountcore.repository.TransactionSubtypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "account.seed.enabled", havingValue = "true", matchIfMissing = true)
public class AccountDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AccountDataInitializer.class);
    private static final String DEFAULT_PASSWORD = "banquito2026";

    private final AccountSubtypeRepository accountSubtypeRepository;
    private final TransactionSubtypeRepository transactionSubtypeRepository;
    private final AccountRepository accountRepository;
    private final InstitutionalAccountRepository institutionalAccountRepository;
    private final CoreParameterRepository coreParameterRepository;
    private final HolidayRepository holidayRepository;
    private final CoreUserRepository coreUserRepository;

    public AccountDataInitializer(AccountSubtypeRepository accountSubtypeRepository,
                                  TransactionSubtypeRepository transactionSubtypeRepository,
                                  AccountRepository accountRepository,
                                  InstitutionalAccountRepository institutionalAccountRepository,
                                  CoreParameterRepository coreParameterRepository,
                                  HolidayRepository holidayRepository,
                                  CoreUserRepository coreUserRepository) {
        this.accountSubtypeRepository = accountSubtypeRepository;
        this.transactionSubtypeRepository = transactionSubtypeRepository;
        this.accountRepository = accountRepository;
        this.institutionalAccountRepository = institutionalAccountRepository;
        this.coreParameterRepository = coreParameterRepository;
        this.holidayRepository = holidayRepository;
        this.coreUserRepository = coreUserRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedAccountSubtypes();
        seedTransactionSubtypes();
        seedInstitutionalAccounts();
        seedParameters();
        seedHolidays();
        seedCoreUsers();
        seedAccounts();
    }

    private void seedAccountSubtypes() {
        if (accountSubtypeRepository.count() > 0) {
            log.info("Account subtypes already loaded ({}); skipping seed.", accountSubtypeRepository.count());
            return;
        }

        List<AccountSubtype> subtypes = readResource("account-subtypes.csv", fields -> {
            requireFields(fields, 4, "account-subtypes.csv");
            AccountSubtype subtype = new AccountSubtype();
            subtype.setCode(fields[0]);
            subtype.setSuperType(AccountSuperType.valueOf(fields[1]));
            subtype.setName(fields[2]);
            subtype.setDescription(fields[3]);
            subtype.setStatus(CatalogStatus.ACTIVO);
            return subtype;
        });
        accountSubtypeRepository.saveAll(subtypes);
        log.info("Seeded {} account subtypes.", subtypes.size());
    }

    private void seedTransactionSubtypes() {
        if (transactionSubtypeRepository.count() > 0) {
            log.info("Transaction subtypes already loaded ({}); skipping seed.", transactionSubtypeRepository.count());
            return;
        }

        List<TransactionSubtype> subtypes = readResource("transaction-subtypes.csv", fields -> {
            requireFields(fields, 2, "transaction-subtypes.csv");
            TransactionSubtype subtype = new TransactionSubtype();
            subtype.setCode(fields[0]);
            subtype.setName(fields[1]);
            subtype.setStatus(CatalogStatus.ACTIVO);
            return subtype;
        });
        transactionSubtypeRepository.saveAll(subtypes);
        log.info("Seeded {} transaction subtypes.", subtypes.size());
    }

    private void seedAccounts() {
        if (accountRepository.count() > 0) {
            log.info("Accounts already loaded ({}); skipping seed.", accountRepository.count());
            return;
        }

        List<Account> accounts = readResource("accounts-seed.csv", fields -> {
            requireFields(fields, 7, "accounts-seed.csv");
            AccountSubtype subtype = accountSubtypeRepository.findByCode(fields[3])
                    .orElseThrow(() -> new IllegalStateException("Account subtype not found: " + fields[3]));

            Account account = new Account();
            account.setAccountNumber(fields[0]);
            account.setCustomerId(Long.valueOf(fields[1]));
            account.setBranchId(Integer.valueOf(fields[2]));
            account.setAccountSubtype(subtype);
            account.setAvailableBalance(new BigDecimal(fields[4]));
            account.setAccountingBalance(new BigDecimal(fields[5]));
            account.setFavorite(Boolean.valueOf(fields[6]));
            account.setStatus(AccountStatus.ACTIVA);
            return account;
        });
        accountRepository.saveAll(accounts);
        log.info("Seeded {} sample accounts.", accounts.size());
    }

    private void seedInstitutionalAccounts() {
        if (institutionalAccountRepository.count() > 0) {
            log.info("Institutional accounts already loaded ({}); skipping seed.",
                    institutionalAccountRepository.count());
            return;
        }

        institutionalAccountRepository.saveAll(List.of(
                institutionalAccount("9900000001", "Boveda Central / Efectivo en Caja", "1000000.00"),
                institutionalAccount("9900000002", "Banco Central / Camara de Compensacion", "500000.00"),
                institutionalAccount("9900000003", "IVA Retenido por Servicios", "0.00"),
                institutionalAccount("9900000004", "Ingresos por Servicios Masivos", "0.00")
        ));
        log.info("Seeded 4 institutional accounts.");
    }

    private void seedParameters() {
        if (coreParameterRepository.count() > 0) {
            log.info("Core parameters already loaded ({}); skipping seed.", coreParameterRepository.count());
            return;
        }

        coreParameterRepository.saveAll(List.of(
                parameter("FECHA_CONTABLE_ACTIVA", "Fecha Contable Activa", LocalDate.now().toString(), "DATE",
                        "Current accounting date."),
                parameter("CUT_OFF_TIME", "Horario de Corte", "20:00", "TIME",
                        "Transactions after this time use the next accounting date."),
                parameter("IVA_RATE", "Tasa IVA Servicios", "0.15", "DECIMAL",
                        "VAT applied to service commissions."),
                parameter("BANCO_CODE", "Codigo Banco BanQuito", "001", "STRING",
                        "Institution routing code."),
                parameter("EOD_STATUS", "Estado EOD", "PENDIENTE", "STRING",
                        "Current end-of-day process status.")
        ));
        log.info("Seeded 5 core parameters.");
    }

    private void seedHolidays() {
        if (holidayRepository.count() > 0) {
            log.info("Holidays already loaded ({}); skipping seed.", holidayRepository.count());
            return;
        }

        holidayRepository.saveAll(List.of(
                holiday("2026-01-01", "Ano Nuevo"),
                holiday("2026-02-16", "Carnaval"),
                holiday("2026-02-17", "Carnaval"),
                holiday("2026-04-03", "Viernes Santo"),
                holiday("2026-05-01", "Dia del Trabajo"),
                holiday("2026-05-24", "Batalla de Pichincha"),
                holiday("2026-08-10", "Primer Grito de Independencia"),
                holiday("2026-10-09", "Independencia de Guayaquil"),
                holiday("2026-11-02", "Dia de Difuntos"),
                holiday("2026-11-03", "Independencia de Cuenca"),
                holiday("2026-12-25", "Navidad")
        ));
        log.info("Seeded 11 holidays.");
    }

    private void seedCoreUsers() {
        if (coreUserRepository.count() > 0) {
            log.info("Core users already loaded ({}); skipping seed.", coreUserRepository.count());
            return;
        }

        String passwordHash = BCrypt.hashpw(DEFAULT_PASSWORD, BCrypt.gensalt());
        coreUserRepository.saveAll(List.of(
                coreUser("cajero.norte", "Carlos Ruiz", 1, "NORTE", passwordHash),
                coreUser("cajero.sur", "Ana Mora", 2, "SUR", passwordHash),
                coreUser("cajero.centro", "Luis Torres", 3, "CENTRO", passwordHash),
                coreUser("cajero.valles", "Rosa Vega", 4, "VALLES", passwordHash)
        ));
        log.info("Seeded 4 teller users. Default local password: {}", DEFAULT_PASSWORD);
    }

    private InstitutionalAccount institutionalAccount(String number, String name, String balance) {
        InstitutionalAccount account = new InstitutionalAccount();
        account.setAccountNumber(number);
        account.setName(name);
        account.setAccountingBalance(new BigDecimal(balance));
        account.setStatus(CatalogStatus.ACTIVO);
        return account;
    }

    private CoreParameter parameter(String code, String name, String value, String type, String description) {
        CoreParameter parameter = new CoreParameter();
        parameter.setCode(code);
        parameter.setName(name);
        parameter.setValueString(value);
        parameter.setDataType(type);
        parameter.setDescription(description);
        return parameter;
    }

    private Holiday holiday(String date, String name) {
        Holiday holiday = new Holiday();
        holiday.setHolidayDate(LocalDate.parse(date));
        holiday.setName(name);
        holiday.setIsWeekend(false);
        return holiday;
    }

    private CoreUser coreUser(String username, String fullName, Integer branchId,
                              String branchCode, String passwordHash) {
        CoreUser user = new CoreUser();
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        user.setFullName(fullName);
        user.setRole("CAJERO");
        user.setStatus("ACTIVO");
        user.setBranchId(branchId);
        user.setBranchCode(branchCode);
        return user;
    }

    private <T> List<T> readResource(String resourceName, RowMapper<T> mapper) {
        List<T> rows = new ArrayList<>();
        ClassPathResource resource = new ClassPathResource(resourceName);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.strip();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                String[] fields = java.util.Arrays.stream(trimmed.split(";", -1))
                        .map(String::strip)
                        .toArray(String[]::new);
                rows.add(mapper.map(fields));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read seed resource " + resourceName, e);
        }
        return rows;
    }

    private void requireFields(String[] fields, int expected, String resourceName) {
        if (fields.length != expected) {
            throw new IllegalStateException(
                    "Malformed row in " + resourceName + ": expected " + expected + " fields");
        }
    }

    @FunctionalInterface
    private interface RowMapper<T> {
        T map(String[] fields);
    }
}
