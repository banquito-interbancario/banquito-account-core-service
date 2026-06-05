package ec.edu.espe.banquito.accountcore.controller;

import ec.edu.espe.banquito.accountcore.dto.AccountSummaryResponseDTO;
import ec.edu.espe.banquito.accountcore.dto.BalanceResponseDTO;
import ec.edu.espe.banquito.accountcore.dto.HealthResponseDTO;
import ec.edu.espe.banquito.accountcore.dto.OperationResponseDTO;
import ec.edu.espe.banquito.accountcore.dto.TellerTransactionReqDTO;
import ec.edu.espe.banquito.accountcore.dto.TransactionHistoryDTO;
import ec.edu.espe.banquito.accountcore.dto.TransferP2PReqDTO;
import ec.edu.espe.banquito.accountcore.dto.TransferResponseDTO;
import ec.edu.espe.banquito.accountcore.exception.AccountNotFoundException;
import ec.edu.espe.banquito.accountcore.repository.AccountRepository;
import ec.edu.espe.banquito.accountcore.service.AccountTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v2/accounts")
@Tag(name = "Accounts", description = "Account balance, transaction history and on-us account operations.")
public class AccountController {

    private static final String CURRENCY = "USD";

    private final AccountRepository accountRepository;
    private final AccountTransactionService transactionService;

    public AccountController(AccountRepository accountRepository, AccountTransactionService transactionService) {
        this.accountRepository = accountRepository;
        this.transactionService = transactionService;
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "List accounts by customer", description = "Returns the accounts owned by a customer so clients can select the origin account.")
    @ApiResponse(responseCode = "200", description = "Accounts returned")
    public ResponseEntity<List<AccountSummaryResponseDTO>> getAccountsByCustomer(
            @Parameter(description = "Customer identifier managed by party-service", example = "2")
            @PathVariable Long customerId) {
        List<AccountSummaryResponseDTO> accounts = accountRepository.findByCustomerIdOrderByAccountNumberAsc(customerId).stream()
                .map(account -> new AccountSummaryResponseDTO(
                        account.getId(),
                        account.getAccountNumber(),
                        account.getCustomerId(),
                        account.getStatus(),
                        account.getAvailableBalance(),
                        account.getAccountingBalance(),
                        CURRENCY
                ))
                .toList();
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{accountId}/balance")
    @Operation(summary = "Get account balance", description = "Returns available and accounting balances for an account.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Balance returned",
                    content = @Content(schema = @Schema(implementation = BalanceResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<BalanceResponseDTO> getBalance(
            @Parameter(description = "Internal account identifier", example = "1")
            @PathVariable Long accountId) {
        return accountRepository.findById(accountId)
                .map(account -> ResponseEntity.ok(new BalanceResponseDTO(
                        account.getId(),
                        account.getAccountNumber(),
                        account.getAvailableBalance(),
                        account.getAccountingBalance(),
                        account.getStatus(),
                        CURRENCY
                )))
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    @GetMapping("/{accountId}/transactions")
    @Operation(summary = "Get account transaction history", description = "Returns paginated transaction history filtered by accounting date.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction history returned",
                    content = @Content(schema = @Schema(implementation = TransactionHistoryDTO.class))),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<TransactionHistoryDTO> getTransactions(
            @Parameter(description = "Internal account identifier", example = "1")
            @PathVariable Long accountId,
            @Parameter(description = "Zero-based page number", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Start accounting date", example = "2026-06-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "End accounting date", example = "2026-06-30")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(transactionService.getTransactionHistory(accountId, from, to, pageable));
    }

    @PostMapping("/teller/deposit")
    @Operation(summary = "Register teller deposit", description = "Credits an active account through a teller deposit operation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deposit registered",
                    content = @Content(schema = @Schema(implementation = OperationResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or inactive account"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "409", description = "Duplicated transaction UUID"),
            @ApiResponse(responseCode = "503", description = "Required core gRPC service unavailable")
    })
    public ResponseEntity<OperationResponseDTO> tellerDeposit(@Valid @RequestBody TellerTransactionReqDTO request) {
        return ResponseEntity.ok(transactionService.executeDeposit(request));
    }

    @PostMapping("/teller/withdrawal")
    @Operation(summary = "Register teller withdrawal", description = "Debits an active account through a teller withdrawal operation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Withdrawal registered",
                    content = @Content(schema = @Schema(implementation = OperationResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request, inactive account or insufficient balance"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "409", description = "Duplicated transaction UUID"),
            @ApiResponse(responseCode = "503", description = "Required core gRPC service unavailable")
    })
    public ResponseEntity<OperationResponseDTO> tellerWithdrawal(@Valid @RequestBody TellerTransactionReqDTO request) {
        return ResponseEntity.ok(transactionService.executeWithdrawal(request));
    }

    @PostMapping("/transfer/p2p")
    @Operation(summary = "Execute P2P transfer", description = "Moves funds from an origin account to an on-us destination account.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transfer executed",
                    content = @Content(schema = @Schema(implementation = TransferResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request, inactive account or insufficient balance"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "409", description = "Duplicated transaction UUID"),
            @ApiResponse(responseCode = "503", description = "Required core gRPC service unavailable")
    })
    public ResponseEntity<TransferResponseDTO> transferP2P(@Valid @RequestBody TransferP2PReqDTO request) {
        return ResponseEntity.ok(transactionService.executeP2PTransfer(request));
    }

    @GetMapping("/health")
    @Operation(summary = "Get account core health", description = "Returns a lightweight service health response.")
    @ApiResponse(responseCode = "200", description = "Service is up",
            content = @Content(schema = @Schema(implementation = HealthResponseDTO.class)))
    public ResponseEntity<HealthResponseDTO> health() {
        return ResponseEntity.ok(new HealthResponseDTO("UP", "account-core-service", "2.0"));
    }
}
