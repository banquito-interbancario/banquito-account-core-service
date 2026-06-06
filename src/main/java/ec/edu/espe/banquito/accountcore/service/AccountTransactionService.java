package ec.edu.espe.banquito.accountcore.service;

import ec.edu.espe.banquito.accountcore.client.AccountingServiceClient;
import ec.edu.espe.banquito.accountcore.client.PartyServiceClient;
import ec.edu.espe.banquito.accountcore.dto.AccountingEntryReqDTO;
import ec.edu.espe.banquito.accountcore.dto.BatchCreditReqDTO;
import ec.edu.espe.banquito.accountcore.dto.BatchCreditResponseDTO;
import ec.edu.espe.banquito.accountcore.dto.CorporateDebitReqDTO;
import ec.edu.espe.banquito.accountcore.dto.CorporateDebitResponseDTO;
import ec.edu.espe.banquito.accountcore.dto.OperationResponseDTO;
import ec.edu.espe.banquito.accountcore.dto.TellerTransactionReqDTO;
import ec.edu.espe.banquito.accountcore.dto.TransactionHistoryDTO;
import ec.edu.espe.banquito.accountcore.dto.TransferP2PReqDTO;
import ec.edu.espe.banquito.accountcore.dto.TransferResponseDTO;
import ec.edu.espe.banquito.accountcore.enums.AccountStatus;
import ec.edu.espe.banquito.accountcore.enums.AccountSuperType;
import ec.edu.espe.banquito.accountcore.enums.TransactionStatus;
import ec.edu.espe.banquito.accountcore.enums.TransactionSubtypeCode;
import ec.edu.espe.banquito.accountcore.enums.TransactionType;
import ec.edu.espe.banquito.accountcore.exception.AccountNotFoundException;
import ec.edu.espe.banquito.accountcore.exception.DuplicateTransactionException;
import ec.edu.espe.banquito.accountcore.exception.InactiveAccountException;
import ec.edu.espe.banquito.accountcore.exception.InsufficientBalanceException;
import ec.edu.espe.banquito.accountcore.model.Account;
import ec.edu.espe.banquito.accountcore.model.AccountTransaction;
import ec.edu.espe.banquito.accountcore.model.TransactionSubtype;
import ec.edu.espe.banquito.accountcore.repository.AccountRepository;
import ec.edu.espe.banquito.accountcore.repository.AccountTransactionRepository;
import ec.edu.espe.banquito.accountcore.repository.TransactionSubtypeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AccountTransactionService {

    private static final String VAULT_ACCOUNT_CODE = "1.1.0.02";
    private static final String PAYMENT_CLEARING_ACCOUNT_CODE = "2.3.0.01";
    private static final String SERVICE_INCOME_ACCOUNT_CODE = "4.1.0.01";
    private static final String VAT_PAYABLE_ACCOUNT_CODE = "2.2.0.01";
    private static final BigDecimal IVA_RATE = new BigDecimal("0.15");

    private final AccountRepository accountRepository;
    private final AccountTransactionRepository transactionRepository;
    private final TransactionSubtypeRepository transactionSubtypeRepository;
    private final AccountingServiceClient accountingServiceClient;
    private final PartyServiceClient partyServiceClient;

    public AccountTransactionService(AccountRepository accountRepository,
                                     AccountTransactionRepository transactionRepository,
                                     TransactionSubtypeRepository transactionSubtypeRepository,
                                     AccountingServiceClient accountingServiceClient,
                                     PartyServiceClient partyServiceClient) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.transactionSubtypeRepository = transactionSubtypeRepository;
        this.accountingServiceClient = accountingServiceClient;
        this.partyServiceClient = partyServiceClient;
    }

    @Transactional(readOnly = true)
    public TransactionHistoryDTO getTransactionHistory(Long accountId, LocalDate from, LocalDate to, Pageable pageable) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }
        Page<AccountTransaction> page = transactionRepository.findHistory(accountId, from, to, pageable);
        List<TransactionHistoryDTO.TransactionHistoryItemDTO> content = page.getContent().stream()
                .map(transaction -> new TransactionHistoryDTO.TransactionHistoryItemDTO(
                        transaction.getTransactionUuid(),
                        transaction.getMovementType().name(),
                        transaction.getAmount(),
                        transaction.getResultingBalance(),
                        transaction.getTransactionDate(),
                        transaction.getAccountingDate(),
                        transaction.getDescription()
                ))
                .toList();
        return new TransactionHistoryDTO(content, page.getTotalElements(), page.getNumber());
    }

    @Transactional
    public OperationResponseDTO executeDeposit(TellerTransactionReqDTO request) {
        validateIdempotency(request.transactionUuid());

        Account account = getAccountForUpdate(request.accountId());
        validateActiveAccount(account);
        partyServiceClient.validateActiveCustomer(account.getCustomerId());

        credit(account, request.amount());
        accountRepository.save(account);

        LocalDate accountingDate = LocalDate.now();
        AccountTransaction transaction = transactionRepository.save(createTransaction(
                account,
                request.amount(),
                TransactionType.CREDIT,
                TransactionSubtypeCode.TELLER_DEPOSIT,
                request.transactionUuid(),
                accountingDate,
                account.getAvailableBalance(),
                descriptionOrDefault(request.reference(), "Teller deposit")
        ));

        accountingServiceClient.registerEntry(new AccountingEntryReqDTO(
                request.transactionUuid(),
                "Teller deposit account " + account.getId(),
                accountingDate,
                List.of(
                        journalLine(VAULT_ACCOUNT_CODE, TransactionType.DEBIT, request.amount(), request.transactionUuid()),
                        journalLine(getCustomerLiabilityAccountCode(account), TransactionType.CREDIT, request.amount(), request.transactionUuid())
                )
        ));

        return toOperationResponse(transaction, account.getAvailableBalance());
    }

    @Transactional
    public OperationResponseDTO executeWithdrawal(TellerTransactionReqDTO request) {
        validateIdempotency(request.transactionUuid());

        Account account = getAccountForUpdate(request.accountId());
        validateActiveAccount(account);
        partyServiceClient.validateActiveCustomer(account.getCustomerId());
        validateSufficientBalance(account, request.amount());

        debit(account, request.amount());
        accountRepository.save(account);

        LocalDate accountingDate = LocalDate.now();
        AccountTransaction transaction = transactionRepository.save(createTransaction(
                account,
                request.amount(),
                TransactionType.DEBIT,
                TransactionSubtypeCode.TELLER_WITHDRAWAL,
                request.transactionUuid(),
                accountingDate,
                account.getAvailableBalance(),
                descriptionOrDefault(request.reference(), "Teller withdrawal")
        ));

        accountingServiceClient.registerEntry(new AccountingEntryReqDTO(
                request.transactionUuid(),
                "Teller withdrawal account " + account.getId(),
                accountingDate,
                List.of(
                        journalLine(getCustomerLiabilityAccountCode(account), TransactionType.DEBIT, request.amount(), request.transactionUuid()),
                        journalLine(VAULT_ACCOUNT_CODE, TransactionType.CREDIT, request.amount(), request.transactionUuid())
                )
        ));

        return toOperationResponse(transaction, account.getAvailableBalance());
    }

    @Transactional
    public TransferResponseDTO executeP2PTransfer(TransferP2PReqDTO request) {
        validateIdempotency(request.transactionUuid());

        Account sourceAccount = getAccountForUpdate(request.originAccountId());
        Account destinationAccount = getAccountForUpdate(request.destinationAccountNumber());

        if (sourceAccount.getAccountNumber().equals(destinationAccount.getAccountNumber())) {
            throw new IllegalArgumentException("Source and destination accounts must be different");
        }

        validateActiveAccount(sourceAccount);
        validateActiveAccount(destinationAccount);
        partyServiceClient.validateActiveCustomer(sourceAccount.getCustomerId());
        partyServiceClient.validateActiveCustomer(destinationAccount.getCustomerId());
        validateSufficientBalance(sourceAccount, request.amount());

        debit(sourceAccount, request.amount());
        credit(destinationAccount, request.amount());
        accountRepository.save(sourceAccount);
        accountRepository.save(destinationAccount);

        LocalDate accountingDate = LocalDate.now();
        AccountTransaction debitTransaction = transactionRepository.save(createTransaction(
                sourceAccount,
                request.amount(),
                TransactionType.DEBIT,
                TransactionSubtypeCode.P2P_OUT,
                request.transactionUuid(),
                accountingDate,
                sourceAccount.getAvailableBalance(),
                descriptionOrDefault(request.reference(), "Internal P2P transfer sent")
        ));
        transactionRepository.save(createTransaction(
                destinationAccount,
                request.amount(),
                TransactionType.CREDIT,
                TransactionSubtypeCode.P2P_IN,
                request.transactionUuid(),
                accountingDate,
                destinationAccount.getAvailableBalance(),
                descriptionOrDefault(request.reference(), "Internal P2P transfer received")
        ));

        accountingServiceClient.registerEntry(new AccountingEntryReqDTO(
                request.transactionUuid(),
                "Internal P2P transfer " + sourceAccount.getAccountNumber() + " to " + destinationAccount.getAccountNumber(),
                accountingDate,
                List.of(
                        journalLine(getCustomerLiabilityAccountCode(sourceAccount), TransactionType.DEBIT, request.amount(), request.transactionUuid()),
                        journalLine(getCustomerLiabilityAccountCode(destinationAccount), TransactionType.CREDIT, request.amount(), request.transactionUuid())
                )
        ));

        return new TransferResponseDTO(
                debitTransaction.getTransactionUuid(),
                sourceAccount.getAvailableBalance(),
                destinationAccount.getAccountNumber(),
                partyServiceClient.getHolderNameByAccount(destinationAccount.getAccountNumber()),
                debitTransaction.getStatus(),
                debitTransaction.getAccountingDate()
        );
    }

    @Transactional
    public BatchCreditResponseDTO executeBatchCredit(BatchCreditReqDTO request) {
        List<BatchCreditResponseDTO.BatchCreditResultDTO> results = new ArrayList<>();

        for (BatchCreditReqDTO.CreditItemDTO creditItem : request.credits()) {
            validateIdempotency(creditItem.transactionUuid());

            Account account = getAccountForUpdate(creditItem.accountId());
            validateActiveAccount(account);
            partyServiceClient.validateActiveCustomer(account.getCustomerId());

            credit(account, creditItem.amount());
            accountRepository.save(account);

            LocalDate accountingDate = LocalDate.now();
            transactionRepository.save(createTransaction(
                    account,
                    creditItem.amount(),
                    TransactionType.CREDIT,
                    TransactionSubtypeCode.BATCH_CREDIT,
                    creditItem.transactionUuid(),
                    accountingDate,
                    account.getAvailableBalance(),
                    descriptionOrDefault(creditItem.reference(), "Batch credit " + request.batchId())
            ));

            accountingServiceClient.registerEntry(new AccountingEntryReqDTO(
                    creditItem.transactionUuid(),
                    "Batch credit " + request.batchId() + " account " + account.getId(),
                    accountingDate,
                    List.of(
                            journalLine(PAYMENT_CLEARING_ACCOUNT_CODE, TransactionType.DEBIT, creditItem.amount(), request.batchId()),
                            journalLine(getCustomerLiabilityAccountCode(account), TransactionType.CREDIT, creditItem.amount(), creditItem.transactionUuid())
                    )
            ));

            results.add(new BatchCreditResponseDTO.BatchCreditResultDTO(
                    creditItem.accountId(),
                    "SUCCESS",
                    creditItem.transactionUuid()
            ));
        }

        return new BatchCreditResponseDTO(request.batchId(), results.size(), 0, results);
    }

    @Transactional
    public CorporateDebitResponseDTO executeCorporateDebit(CorporateDebitReqDTO request) {
        validateIdempotency(request.transactionUuid());

        Account account = getAccountForUpdate(request.accountId());
        validateActiveAccount(account);
        partyServiceClient.validateActiveCustomer(account.getCustomerId());

        BigDecimal ivaAmount = request.commissionAmount()
                .multiply(IVA_RATE)
                .divide(BigDecimal.ONE.add(IVA_RATE), 2, RoundingMode.HALF_UP);
        BigDecimal commissionNet = request.commissionAmount().subtract(ivaAmount);
        BigDecimal debitedAmount = request.totalAmount().add(request.commissionAmount());
        validateSufficientBalance(account, debitedAmount);

        debit(account, debitedAmount);
        accountRepository.save(account);

        LocalDate accountingDate = LocalDate.now();
        AccountTransaction transaction = transactionRepository.save(createTransaction(
                account,
                debitedAmount,
                TransactionType.DEBIT,
                TransactionSubtypeCode.CORPORATE_DEBIT,
                request.transactionUuid(),
                accountingDate,
                account.getAvailableBalance(),
                "Corporate debit batch " + request.batchId()
        ));

        accountingServiceClient.registerEntry(new AccountingEntryReqDTO(
                request.transactionUuid(),
                "Corporate debit batch " + request.batchId(),
                accountingDate,
                List.of(
                        journalLine(getCustomerLiabilityAccountCode(account), TransactionType.DEBIT, debitedAmount, request.transactionUuid()),
                        journalLine(PAYMENT_CLEARING_ACCOUNT_CODE, TransactionType.CREDIT, request.totalAmount(), request.batchId()),
                        journalLine(SERVICE_INCOME_ACCOUNT_CODE, TransactionType.CREDIT, commissionNet, request.transactionUuid()),
                        journalLine(VAT_PAYABLE_ACCOUNT_CODE, TransactionType.CREDIT, ivaAmount, request.transactionUuid())
                )
        ));

        return new CorporateDebitResponseDTO(
                transaction.getTransactionUuid(),
                debitedAmount,
                commissionNet,
                ivaAmount,
                transaction.getStatus(),
                transaction.getAccountingDate()
        );
    }

    private void validateIdempotency(String transactionUuid) {
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        if (transactionRepository.existsByTransactionUuidAndTransactionDateAfter(transactionUuid, from)) {
            throw new DuplicateTransactionException(transactionUuid);
        }
    }

    private Account getAccountForUpdate(Long accountId) {
        return accountRepository.findWithLockById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    private Account getAccountForUpdate(String accountNumber) {
        return accountRepository.findWithLockByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    private void validateActiveAccount(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InactiveAccountException(account.getAccountNumber());
        }
    }

    private void validateSufficientBalance(Account account, BigDecimal amount) {
        if (account.getAvailableBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(account.getAccountNumber());
        }
    }

    private void debit(Account account, BigDecimal amount) {
        account.setAvailableBalance(account.getAvailableBalance().subtract(amount));
        account.setAccountingBalance(account.getAccountingBalance().subtract(amount));
    }

    private void credit(Account account, BigDecimal amount) {
        account.setAvailableBalance(account.getAvailableBalance().add(amount));
        account.setAccountingBalance(account.getAccountingBalance().add(amount));
    }

    private AccountTransaction createTransaction(Account account,
                                                 BigDecimal amount,
                                                 TransactionType transactionType,
                                                 TransactionSubtypeCode transactionSubtype,
                                                 String transactionUuid,
                                                 LocalDate accountingDate,
                                                 BigDecimal resultingBalance,
                                                 String description) {
        AccountTransaction transaction = new AccountTransaction();
        transaction.setAccount(account);
        transaction.setAmount(amount);
        transaction.setMovementType(transactionType);
        transaction.setTransactionSubtype(getTransactionSubtype(transactionSubtype));
        transaction.setTransactionUuid(transactionUuid);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setAccountingDate(accountingDate);
        transaction.setResultingBalance(resultingBalance);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setDescription(description);
        return transaction;
    }

    private OperationResponseDTO toOperationResponse(AccountTransaction transaction, BigDecimal newBalance) {
        return new OperationResponseDTO(
                transaction.getTransactionUuid(),
                transaction.getAccountingDate(),
                newBalance,
                transaction.getStatus(),
                transaction.getTransactionDate()
        );
    }

    private AccountingEntryReqDTO.JournalLineDTO journalLine(String accountCode,
                                                             TransactionType movementType,
                                                             BigDecimal amount,
                                                             String reference) {
        return new AccountingEntryReqDTO.JournalLineDTO(accountCode, movementType.name(), amount, reference);
    }

    private String getCustomerLiabilityAccountCode(Account account) {
        AccountSuperType superType = account.getAccountSubtype().getSuperType();
        return superType == AccountSuperType.SAVINGS ? "2.1.0.01" : "2.1.0.02";
    }

    private TransactionSubtype getTransactionSubtype(TransactionSubtypeCode subtypeCode) {
        return transactionSubtypeRepository.findByCode(subtypeCode.databaseCode())
                .orElseThrow(() -> new IllegalStateException("Transaction subtype is not configured: " + subtypeCode.databaseCode()));
    }

    private String descriptionOrDefault(String description, String defaultDescription) {
        return description == null || description.isBlank() ? defaultDescription : description;
    }
}
