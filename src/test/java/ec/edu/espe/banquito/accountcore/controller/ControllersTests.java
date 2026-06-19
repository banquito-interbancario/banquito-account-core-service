package ec.edu.espe.banquito.accountcore.controller;

import ec.edu.espe.banquito.accountcore.dto.BatchCreditReqDTO;
import ec.edu.espe.banquito.accountcore.dto.BatchCreditResponseDTO;
import ec.edu.espe.banquito.accountcore.dto.CorporateDebitReqDTO;
import ec.edu.espe.banquito.accountcore.dto.CorporateDebitResponseDTO;
import ec.edu.espe.banquito.accountcore.dto.FavoriteAccountResponseDTO;
import ec.edu.espe.banquito.accountcore.dto.HolidayCheckResponseDTO;
import ec.edu.espe.banquito.accountcore.dto.OperationResponseDTO;
import ec.edu.espe.banquito.accountcore.dto.StaffLoginRequestDTO;
import ec.edu.espe.banquito.accountcore.dto.StaffLoginResponseDTO;
import ec.edu.espe.banquito.accountcore.dto.TellerTransactionReqDTO;
import ec.edu.espe.banquito.accountcore.dto.TransactionHistoryDTO;
import ec.edu.espe.banquito.accountcore.dto.TransferP2PReqDTO;
import ec.edu.espe.banquito.accountcore.dto.TransferResponseDTO;
import ec.edu.espe.banquito.accountcore.enums.AccountStatus;
import ec.edu.espe.banquito.accountcore.model.Account;
import ec.edu.espe.banquito.accountcore.repository.AccountRepository;
import ec.edu.espe.banquito.accountcore.repository.AccountSubtypeRepository;
import ec.edu.espe.banquito.accountcore.service.AccountQueryService;
import ec.edu.espe.banquito.accountcore.service.AccountTransactionService;
import ec.edu.espe.banquito.accountcore.service.CalendarQueryService;
import ec.edu.espe.banquito.accountcore.service.CoreUserAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ControllersTests {

    @Test
    void delegatesAuthentication() {
        CoreUserAuthService service = mock(CoreUserAuthService.class);
        AuthController controller = new AuthController(service);
        StaffLoginRequestDTO request = new StaffLoginRequestDTO("cajero.norte", "secret");
        StaffLoginResponseDTO expected =
                new StaffLoginResponseDTO(1, "cajero.norte", "Cashier", "CAJERO", 1, "NORTE", "ACTIVO");
        when(service.loginStaff(request)).thenReturn(expected);

        assertSame(expected, controller.loginStaff(request).getBody());
    }

    @Test
    void delegatesHolidayCheck() {
        CalendarQueryService service = mock(CalendarQueryService.class);
        CalendarController controller = new CalendarController(service, mock(ec.edu.espe.banquito.accountcore.service.AccountingDateService.class));
        LocalDate date = LocalDate.of(2026, Month.DECEMBER, 25);
        HolidayCheckResponseDTO expected = new HolidayCheckResponseDTO(date, true, "Navidad", false);
        when(service.checkHoliday(date)).thenReturn(expected);

        assertSame(expected, controller.checkHoliday(date).getBody());
    }

    @Test
    void delegatesPaymentIntegrationOperations() {
        AccountTransactionService service = mock(AccountTransactionService.class);
        CorePaymentIntegrationController controller = new CorePaymentIntegrationController(service);
        BatchCreditReqDTO batchRequest = new BatchCreditReqDTO(
                "batch",
                List.of(new BatchCreditReqDTO.CreditItemDTO(
                        "2200000001", BigDecimal.TEN, null, "credit"))
        );
        BatchCreditResponseDTO batchResponse =
                new BatchCreditResponseDTO("batch", 1, 0, List.of());
        CorporateDebitReqDTO debitRequest = new CorporateDebitReqDTO(
                "2200000001", BigDecimal.TEN, BigDecimal.ONE, "batch", "debit");
        CorporateDebitResponseDTO debitResponse = mock(CorporateDebitResponseDTO.class);
        when(service.executeBatchCredit(batchRequest)).thenReturn(batchResponse);
        when(service.executeCorporateDebit(debitRequest)).thenReturn(debitResponse);

        assertSame(batchResponse, controller.batchCredit(batchRequest).getBody());
        assertSame(debitResponse, controller.corporateDebit(debitRequest).getBody());
    }

    @Test
    void returnsCustomerAccountsAndBalance() {
        AccountRepository repository = mock(AccountRepository.class);
        AccountSubtypeRepository subtypeRepository = mock(AccountSubtypeRepository.class);
        AccountQueryService queryService = mock(AccountQueryService.class);
        AccountTransactionService transactionService = mock(AccountTransactionService.class);
        AccountController controller = new AccountController(repository, subtypeRepository, queryService, transactionService, mock(ec.edu.espe.banquito.accountcore.service.AccountOpenService.class), mock(ec.edu.espe.banquito.accountcore.service.AccountStatusService.class));
        Account account = account();
        when(repository.findByCustomerIdOrderByAccountNumberAsc(1L)).thenReturn(List.of(account));
        when(repository.findById(1L)).thenReturn(Optional.of(account));
        when(queryService.resolveAccount("1")).thenReturn(account);

        var accounts = controller.getAccountsByCustomer(1L).getBody();
        var balance = controller.getBalance("1").getBody();

        assertEquals("2200000001", accounts.getFirst().accountNumber());
        assertEquals(new BigDecimal("100.00"), balance.availableBalance());
    }

    @Test
    void delegatesFavoriteHistoryAndTransactions() {
        AccountRepository repository = mock(AccountRepository.class);
        AccountSubtypeRepository subtypeRepository = mock(AccountSubtypeRepository.class);
        AccountQueryService queryService = mock(AccountQueryService.class);
        AccountTransactionService transactionService = mock(AccountTransactionService.class);
        AccountController controller = new AccountController(repository, subtypeRepository, queryService, transactionService, mock(ec.edu.espe.banquito.accountcore.service.AccountOpenService.class), mock(ec.edu.espe.banquito.accountcore.service.AccountStatusService.class));
        FavoriteAccountResponseDTO favorite = mock(FavoriteAccountResponseDTO.class);
        TransactionHistoryDTO history = mock(TransactionHistoryDTO.class);
        OperationResponseDTO operation = mock(OperationResponseDTO.class);
        TransferResponseDTO transfer = mock(TransferResponseDTO.class);
        TellerTransactionReqDTO tellerRequest =
                new TellerTransactionReqDTO(1L, BigDecimal.TEN, 1L, 1L, "teller", null);
        TransferP2PReqDTO transferRequest =
                new TransferP2PReqDTO(1L, "2200000002", BigDecimal.TEN, "transfer", null);
        when(queryService.getFavoriteAccount(1L)).thenReturn(favorite);
        when(queryService.resolveAccount("1")).thenReturn(account());
        when(transactionService.getTransactionHistory(
                any(Long.class), any(), any(), any(Pageable.class))).thenReturn(history);
        when(transactionService.executeDeposit(tellerRequest)).thenReturn(operation);
        when(transactionService.executeWithdrawal(tellerRequest)).thenReturn(operation);
        when(transactionService.executeP2PTransfer(transferRequest)).thenReturn(transfer);

        assertSame(favorite, controller.getFavoriteAccount(1L).getBody());
        assertSame(history, controller.getTransactions("1", 0, 20, null, null).getBody());
        assertSame(operation, controller.tellerDeposit(tellerRequest).getBody());
        assertSame(operation, controller.tellerWithdrawal(tellerRequest).getBody());
        assertSame(transfer, controller.transferP2P(transferRequest).getBody());
        assertEquals("UP", controller.health().getBody().status());
        verify(transactionService).executeP2PTransfer(transferRequest);
    }

    private Account account() {
        Account account = new Account();
        account.setId(1L);
        account.setAccountNumber("2200000001");
        account.setCustomerId(1L);
        account.setStatus(AccountStatus.ACTIVA);
        account.setAvailableBalance(new BigDecimal("100.00"));
        account.setAccountingBalance(new BigDecimal("100.00"));
        return account;
    }
}
