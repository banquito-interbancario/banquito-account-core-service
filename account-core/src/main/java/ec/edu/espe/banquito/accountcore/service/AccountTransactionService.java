package ec.edu.espe.banquito.accountcore.service;

import ec.edu.espe.banquito.accountcore.client.AccountingServiceClient;
import ec.edu.espe.banquito.accountcore.dto.AccountingEntryReqDTO;
import ec.edu.espe.banquito.accountcore.dto.CorporateDebitReqDTO;
import ec.edu.espe.banquito.accountcore.dto.TellerTransactionReqDTO;
import ec.edu.espe.banquito.accountcore.model.Account;
import ec.edu.espe.banquito.accountcore.model.AccountTransaction;
import ec.edu.espe.banquito.accountcore.repository.AccountRepository;
import ec.edu.espe.banquito.accountcore.repository.AccountTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AccountTransactionService {

    private final AccountRepository accountRepository;
    private final AccountTransactionRepository transactionRepository;
    private final AccountingServiceClient accountingServiceClient;

    public AccountTransactionService(AccountRepository accountRepository,
                                     AccountTransactionRepository transactionRepository,
                                     AccountingServiceClient accountingServiceClient) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.accountingServiceClient = accountingServiceClient;
    }

    /**
     * RF-02: Depósito en Efectivo por Ventanilla
     */
    @Transactional(rollbackFor = Exception.class)
    public void executeDeposit(TellerTransactionReqDTO dto) {
        validateIdempotency(dto.transactionUuid());

        Account account = accountRepository.findByAccountNumber(dto.accountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Cuenta no encontrada"));

        if (!"ACTIVA".equals(account.getStatus())) {
            throw new IllegalStateException("La cuenta no se encuentra ACTIVA");
        }

        LocalDate accountingDate = calculateAccountingDate();

        // 1. Afectar saldo local comercial
        account.setAvailableBalance(account.getAvailableBalance().add(dto.amount()));
        account.setAccountingBalance(account.getAccountingBalance().add(dto.amount()));
        accountRepository.save(account);

        // 2. Registrar transacción local (Se guardará en la partición mensual correspondiente)
        AccountTransaction tx = createLocalTransaction(account, dto.amount(), "CREDIT", "DEP", dto.transactionUuid(), accountingDate);
        transactionRepository.save(tx);

        // 3. Orquestar Asiento Contable - Contrapartida obligatoria: Bóveda Central (1.1.0.02)
        List<AccountingEntryReqDTO.JournalLineDTO> lines = List.of(
                new AccountingEntryReqDTO.JournalLineDTO("1.1.0.02", "DEBIT", dto.amount(), "Depósito Ventanilla - Efectivo en Caja"),
                new AccountingEntryReqDTO.JournalLineDTO(getAccountClassCode(account), "CREDIT", dto.amount(), "Abono Cuenta Cliente")
        );

        AccountingEntryReqDTO entry = new AccountingEntryReqDTO(dto.transactionUuid(), "Depósito Ventanilla Cuenta " + dto.accountNumber(), lines);

        // Si esta llamada falla o da timeout, @Transactional ejecuta el REVERSO automático local
        accountingServiceClient.sendAccountingEntry(entry);
    }

    /**
     * RF-04: Débito Corporativo y desglose automático de comisión e IVA
     */
    @Transactional(rollbackFor = Exception.class)
    public void executeCorporateDebit(CorporateDebitReqDTO dto) {
        validateIdempotency(dto.transactionUuid());

        Account companyAccount = accountRepository.findByAccountNumber(dto.accountId())
                .orElseThrow(() -> new IllegalArgumentException("Cuenta de la empresa no encontrada"));

        BigDecimal totalDebit = dto.totalAmount().add(dto.commissionAmount());

        // Regla de negocio: Permitir sobregiro si se especifica, o validar saldo disponible
        if (companyAccount.getAvailableBalance().compareTo(totalDebit) < 0) {
            // Nota de negocio del RF-07 Switch permite sobregiro si es comisión final, ajusta según control estricto
            companyAccount.setAvailableBalance(companyAccount.getAvailableBalance().subtract(totalDebit));
        } else {
            companyAccount.setAvailableBalance(companyAccount.getAvailableBalance().subtract(totalDebit));
        }
        companyAccount.setAccountingBalance(companyAccount.getAccountingBalance().subtract(totalDebit));
        accountRepository.save(companyAccount);

        LocalDate accountingDate = calculateAccountingDate();
        AccountTransaction tx = createLocalTransaction(companyAccount, totalDebit, "DEBIT", "DEB_CORP", dto.transactionUuid(), accountingDate);
        transactionRepository.save(tx);

        // Cálculos de Impuestos internos obligatorios (RF-04)
        BigDecimal commissionSubtotal = dto.commissionAmount();
        BigDecimal ivaAmount = commissionSubtotal.multiply(new BigDecimal("0.15"));
        BigDecimal totalCommissionWithIva = commissionSubtotal.add(ivaAmount);

        // Construir líneas del asiento contable de partida doble delegando la lógica financiera
        List<AccountingEntryReqDTO.JournalLineDTO> lines = new ArrayList<>();
        lines.add(new AccountingEntryReqDTO.JournalLineDTO(getAccountClassCode(companyAccount), "DEBIT", dto.totalAmount().add(totalCommissionWithIva), "Débito Global Nómina y Servicios"));
        lines.add(new AccountingEntryReqDTO.JournalLineDTO("4.1.0.01", "CREDIT", commissionSubtotal, "Ingresos por Servicios Masivos"));
        lines.add(new AccountingEntryReqDTO.JournalLineDTO("2.2.0.01", "CREDIT", ivaAmount, "Pasivo IVA Retenido por Servicios"));

        AccountingEntryReqDTO entry = new AccountingEntryReqDTO(dto.transactionUuid(), "Liquidación Lote Pagos Masivos Empresa " + dto.accountId(), lines);

        accountingServiceClient.sendAccountingEntry(entry);
    }

    /**
     * RF-08: Gestión de Fecha Contable y Horario de Corte (20:00)
     */
    private LocalDate calculateAccountingDate() {
        LocalTime now = LocalTime.now();
        // Reemplazar idealmente obteniendo el parámetro dinámico 'FECHA_CONTABLE_ACTIVA' de tu tabla CORE_PARAMETER
        LocalDate activeAccountingDate = LocalDate.now();

        if (now.isAfter(LocalTime.of(20, 0))) {
            // Si pasa de las 20:00, se mueve al siguiente día hábil (en producción validar con la tabla HOLIDAY)
            return activeAccountingDate.plusDays(1);
        }
        return activeAccountingDate;
    }

    private void validateIdempotency(String uuid) {
        if (transactionRepository.existsByTransactionUuidAndTransactionDateAfter(uuid, LocalDateTime.now().minusDays(1))) {
            // Lanza excepción que se mapeará a un HTTP 409 Conflict en el controlador
            throw new IllegalStateException("TRANSACTION_UUID_DUPLICATED");
        }
    }

    private AccountTransaction createLocalTransaction(Account account, BigDecimal amount, String type, String subtype, String uuid, LocalDate accountingDate) {
        AccountTransaction tx = new AccountTransaction();
        tx.setAccount(account);
        tx.setAmount(amount);
        tx.setTransactionType(type);
        tx.setTransactionSubtype(subtype);
        tx.setTransactionUuid(uuid);
        tx.setTransactionDate(LocalDateTime.now());
        tx.setAccountingDate(accountingDate);
        return tx;
    }

    private String getAccountClassCode(Account account) {
        return "AHORROS".equalsIgnoreCase(account.getAccountType()) ? "2.1.0.01" : "2.1.0.02";
    }
}