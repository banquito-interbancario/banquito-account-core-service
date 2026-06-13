package ec.edu.espe.banquito.accountcore.service;

import ec.edu.espe.banquito.accountcore.client.AccountingServiceClient;
import ec.edu.espe.banquito.accountcore.client.PartyServiceClient;
import ec.edu.espe.banquito.accountcore.config.AccountingRulesProperties;
import ec.edu.espe.banquito.accountcore.dto.AccountingOperationReqDTO;
import ec.edu.espe.banquito.accountcore.dto.BatchCreditReqDTO;
import ec.edu.espe.banquito.accountcore.dto.CorporateDebitReqDTO;
import ec.edu.espe.banquito.accountcore.dto.TellerTransactionReqDTO;
import ec.edu.espe.banquito.accountcore.dto.TransferP2PReqDTO;
import ec.edu.espe.banquito.accountcore.enums.AccountStatus;
import ec.edu.espe.banquito.accountcore.enums.AccountSuperType;
import ec.edu.espe.banquito.accountcore.enums.AccountingOperationType;
import ec.edu.espe.banquito.accountcore.enums.AccountingProductType;
import ec.edu.espe.banquito.accountcore.enums.TransactionStatus;
import ec.edu.espe.banquito.accountcore.enums.TransactionType;
import ec.edu.espe.banquito.accountcore.exception.AccountNotFoundException;
import ec.edu.espe.banquito.accountcore.exception.DuplicateTransactionException;
import ec.edu.espe.banquito.accountcore.exception.InactiveAccountException;
import ec.edu.espe.banquito.accountcore.exception.InsufficientBalanceException;
import ec.edu.espe.banquito.accountcore.model.Account;
import ec.edu.espe.banquito.accountcore.model.AccountSubtype;
import ec.edu.espe.banquito.accountcore.model.AccountTransaction;
import ec.edu.espe.banquito.accountcore.model.TransactionSubtype;
import ec.edu.espe.banquito.accountcore.repository.AccountRepository;
import ec.edu.espe.banquito.accountcore.repository.AccountTransactionRepository;
import ec.edu.espe.banquito.accountcore.repository.TransactionSubtypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountTransactionServiceTests {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountTransactionRepository transactionRepository;
    @Mock
    private TransactionSubtypeRepository transactionSubtypeRepository;
    @Mock
    private AccountingServiceClient accountingServiceClient;
    @Mock
    private PartyServiceClient partyServiceClient;

    private AccountTransactionService service;

    @BeforeEach
    void setUp() {
        AccountingRulesProperties rules = new AccountingRulesProperties(
                new BigDecimal("0.15")
        );
        service = new AccountTransactionService(
                accountRepository,
                transactionRepository,
                transactionSubtypeRepository,
                accountingServiceClient,
                partyServiceClient,
                rules
        );

        lenient().when(transactionRepository.existsByTransactionUuidAndTransactionDateAfter(anyString(), any()))
                .thenReturn(false);
        lenient().when(transactionSubtypeRepository.findByCode(anyString()))
                .thenAnswer(invocation -> Optional.of(transactionSubtype(invocation.getArgument(0))));
        lenient().when(transactionRepository.save(any(AccountTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void returnsTransactionHistory() {
        Account account = account(1L, "2200000001", 1L, "100.00", AccountSuperType.AHORROS);
        AccountTransaction transaction = transaction(account, "history-1", TransactionType.CREDITO, "25.00");
        PageRequest pageable = PageRequest.of(0, 20);
        when(accountRepository.existsById(1L)).thenReturn(true);
        when(transactionRepository.findHistory(1L, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(transaction), pageable, 1));

        var response = service.getTransactionHistory(1L, null, null, pageable);

        assertEquals(1, response.totalElements());
        assertEquals("history-1", response.content().getFirst().transactionUuid());
        assertEquals("CREDITO", response.content().getFirst().movementType());
    }

    @Test
    void rejectsHistoryForUnknownAccount() {
        when(accountRepository.existsById(99L)).thenReturn(false);
        PageRequest pageable = PageRequest.of(0, 20);

        assertThrows(
                AccountNotFoundException.class,
                () -> service.getTransactionHistory(99L, null, null, pageable)
        );
    }

    @Test
    void executesDepositAndRegistersAccountingEntry() {
        Account account = account(1L, "2200000001", 1L, "100.00", AccountSuperType.AHORROS);
        when(accountRepository.findWithLockById(1L)).thenReturn(Optional.of(account));

        var response = service.executeDeposit(
                new TellerTransactionReqDTO(1L, new BigDecimal("25.00"), 10L, 1L, "deposit-1", null)
        );

        assertEquals(new BigDecimal("125.00"), response.newBalance());
        assertEquals(TransactionStatus.COMPLETADA, response.status());
        verify(partyServiceClient).validateActiveCustomer(1L);
        AccountingOperationReqDTO operation = capturedAccountingOperation();
        assertEquals(AccountingOperationType.TELLER_DEPOSIT, operation.operationType());
        assertEquals(AccountingProductType.SAVINGS, operation.accountProductType());
        assertEquals(new BigDecimal("25.00"), operation.amount());
    }

    @Test
    void executesWithdrawalForCurrentAccount() {
        Account account = account(2L, "2200000002", 2L, "100.00", AccountSuperType.CORRIENTE);
        when(accountRepository.findWithLockById(2L)).thenReturn(Optional.of(account));

        var response = service.executeWithdrawal(
                new TellerTransactionReqDTO(2L, new BigDecimal("30.00"), 10L, 1L, "withdrawal-1", "Cash")
        );

        assertEquals(new BigDecimal("70.00"), response.newBalance());
        AccountingOperationReqDTO operation = capturedAccountingOperation();
        assertEquals(AccountingOperationType.TELLER_WITHDRAWAL, operation.operationType());
        assertEquals(AccountingProductType.CHECKING, operation.accountProductType());
    }

    @Test
    void executesP2PTransferAndReturnsHolder() {
        Account source = account(1L, "2200000001", 1L, "100.00", AccountSuperType.AHORROS);
        Account destination = account(2L, "2200000002", 2L, "20.00", AccountSuperType.CORRIENTE);
        when(accountRepository.findWithLockById(1L)).thenReturn(Optional.of(source));
        when(accountRepository.findWithLockByAccountNumber("2200000002")).thenReturn(Optional.of(destination));
        when(partyServiceClient.getHolderNameByAccount("2200000002")).thenReturn("Juan Lopez");

        var response = service.executeP2PTransfer(
                new TransferP2PReqDTO(1L, "2200000002", new BigDecimal("40.00"), "p2p-1", "")
        );

        assertEquals(new BigDecimal("60.00"), response.originNewBalance());
        assertEquals("Juan Lopez", response.destinationHolderName());
        assertEquals(new BigDecimal("60.00"), destination.getAvailableBalance());
        AccountingOperationReqDTO operation = capturedAccountingOperation();
        assertEquals(AccountingOperationType.P2P_TRANSFER, operation.operationType());
        assertEquals(AccountingProductType.SAVINGS, operation.accountProductType());
        assertEquals(BigDecimal.ZERO, operation.commissionAmount());
    }

    @Test
    void executesBatchCreditForEveryItem() {
        Account savings = account(1L, "2200000001", 1L, "100.00", AccountSuperType.AHORROS);
        Account current = account(2L, "2200000002", 2L, "200.00", AccountSuperType.CORRIENTE);
        when(accountRepository.findWithLockByAccountNumber("2200000001")).thenReturn(Optional.of(savings));
        when(accountRepository.findWithLockByAccountNumber("2200000002")).thenReturn(Optional.of(current));

        var response = service.executeBatchCredit(new BatchCreditReqDTO(
                "batch-1",
                List.of(
                        new BatchCreditReqDTO.CreditItemDTO(
                                "2200000001", new BigDecimal("10.00"), "Payroll", "credit-1"),
                        new BatchCreditReqDTO.CreditItemDTO(
                                "2200000002", new BigDecimal("20.00"), null, "credit-2")
                )
        ));

        assertEquals(2, response.processed());
        assertEquals(0, response.failed());
        assertEquals(new BigDecimal("110.00"), savings.getAvailableBalance());
        assertEquals(new BigDecimal("220.00"), current.getAvailableBalance());
        verify(accountingServiceClient, org.mockito.Mockito.times(2)).postOperation(any());
    }

    @Test
    void executesCorporateDebitWithCommissionAndVat() {
        Account account = account(3L, "2200000003", 3L, "1000.00", AccountSuperType.CORRIENTE);
        when(accountRepository.findWithLockByAccountNumber("2200000003")).thenReturn(Optional.of(account));

        var response = service.executeCorporateDebit(new CorporateDebitReqDTO(
                "2200000003",
                new BigDecimal("100.00"),
                new BigDecimal("11.50"),
                "batch-2",
                "corporate-1"
        ));

        assertEquals(new BigDecimal("111.50"), response.debitedAmount());
        assertEquals(new BigDecimal("10.00"), response.commissionNet());
        assertEquals(new BigDecimal("1.50"), response.ivaAmount());
        assertEquals(new BigDecimal("888.50"), account.getAvailableBalance());
        AccountingOperationReqDTO operation = capturedAccountingOperation();
        assertEquals(AccountingOperationType.CORPORATE_DEBIT, operation.operationType());
        assertEquals(AccountingProductType.CHECKING, operation.accountProductType());
        assertEquals(new BigDecimal("100.00"), operation.amount());
        assertEquals(new BigDecimal("10.00"), operation.commissionAmount());
    }

    @Test
    void rejectsDuplicateTransaction() {
        when(transactionRepository.existsByTransactionUuidAndTransactionDateAfter(anyString(), any()))
                .thenReturn(true);
        TellerTransactionReqDTO request =
                new TellerTransactionReqDTO(1L, BigDecimal.ONE, 1L, 1L, "duplicate", null);

        assertThrows(
                DuplicateTransactionException.class,
                () -> service.executeDeposit(request)
        );
        verify(accountRepository, never()).findWithLockById(any());
    }

    @Test
    void rejectsInactiveAccount() {
        Account account = account(1L, "2200000001", 1L, "100.00", AccountSuperType.AHORROS);
        account.setStatus(AccountStatus.INACTIVA);
        when(accountRepository.findWithLockById(1L)).thenReturn(Optional.of(account));
        TellerTransactionReqDTO request =
                new TellerTransactionReqDTO(1L, BigDecimal.ONE, 1L, 1L, "inactive", null);

        assertThrows(
                InactiveAccountException.class,
                () -> service.executeDeposit(request)
        );
    }

    @Test
    void rejectsInsufficientBalance() {
        Account account = account(1L, "2200000001", 1L, "5.00", AccountSuperType.AHORROS);
        when(accountRepository.findWithLockById(1L)).thenReturn(Optional.of(account));
        TellerTransactionReqDTO request =
                new TellerTransactionReqDTO(1L, BigDecimal.TEN, 1L, 1L, "insufficient", null);

        assertThrows(
                InsufficientBalanceException.class,
                () -> service.executeWithdrawal(request)
        );
    }

    @Test
    void rejectsTransferToSameAccount() {
        Account account = account(1L, "2200000001", 1L, "100.00", AccountSuperType.AHORROS);
        when(accountRepository.findWithLockById(1L)).thenReturn(Optional.of(account));
        when(accountRepository.findWithLockByAccountNumber("2200000001")).thenReturn(Optional.of(account));
        TransferP2PReqDTO request =
                new TransferP2PReqDTO(1L, "2200000001", BigDecimal.TEN, "same", null);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.executeP2PTransfer(request)
        );
    }

    @Test
    void rejectsUnknownAccountAndMissingSubtype() {
        when(accountRepository.findWithLockById(88L)).thenReturn(Optional.empty());
        TellerTransactionReqDTO missingAccountRequest =
                new TellerTransactionReqDTO(88L, BigDecimal.ONE, 1L, 1L, "missing", null);
        assertThrows(
                AccountNotFoundException.class,
                () -> service.executeDeposit(missingAccountRequest)
        );

        Account account = account(1L, "2200000001", 1L, "100.00", AccountSuperType.AHORROS);
        when(accountRepository.findWithLockById(1L)).thenReturn(Optional.of(account));
        when(transactionSubtypeRepository.findByCode("DEP_VEN")).thenReturn(Optional.empty());
        TellerTransactionReqDTO missingSubtypeRequest =
                new TellerTransactionReqDTO(1L, BigDecimal.ONE, 1L, 1L, "subtype", null);
        assertThrows(
                IllegalStateException.class,
                () -> service.executeDeposit(missingSubtypeRequest)
        );
    }

    private AccountingOperationReqDTO capturedAccountingOperation() {
        ArgumentCaptor<AccountingOperationReqDTO> captor = ArgumentCaptor.forClass(AccountingOperationReqDTO.class);
        verify(accountingServiceClient).postOperation(captor.capture());
        return captor.getValue();
    }

    private Account account(Long id,
                            String number,
                            Long customerId,
                            String balance,
                            AccountSuperType superType) {
        AccountSubtype subtype = new AccountSubtype();
        subtype.setSuperType(superType);

        Account account = new Account();
        account.setId(id);
        account.setAccountNumber(number);
        account.setCustomerId(customerId);
        account.setAccountSubtype(subtype);
        account.setAvailableBalance(new BigDecimal(balance));
        account.setAccountingBalance(new BigDecimal(balance));
        account.setStatus(AccountStatus.ACTIVA);
        return account;
    }

    private TransactionSubtype transactionSubtype(String code) {
        TransactionSubtype subtype = new TransactionSubtype();
        subtype.setCode(code);
        subtype.setName(code);
        return subtype;
    }

    private AccountTransaction transaction(Account account,
                                           String uuid,
                                           TransactionType type,
                                           String amount) {
        AccountTransaction transaction = new AccountTransaction();
        transaction.setAccount(account);
        transaction.setTransactionUuid(uuid);
        transaction.setMovementType(type);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setResultingBalance(account.getAvailableBalance());
        transaction.setTransactionDate(LocalDateTime.of(2026, Month.JUNE, 11, 10, 0));
        transaction.setAccountingDate(LocalDate.of(2026, Month.JUNE, 11));
        transaction.setDescription("History");
        transaction.setStatus(TransactionStatus.COMPLETADA);
        return transaction;
    }
}
