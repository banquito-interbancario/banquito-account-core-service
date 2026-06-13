package ec.edu.espe.banquito.accountcore.service;

import ec.edu.espe.banquito.accountcore.client.AccountingServiceClient;
import ec.edu.espe.banquito.accountcore.client.PartyServiceClient;
import ec.edu.espe.banquito.accountcore.config.AccountingRulesProperties;
import ec.edu.espe.banquito.accountcore.dto.AccountingOperationReqDTO;
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
import ec.edu.espe.banquito.accountcore.enums.AccountingOperationType;
import ec.edu.espe.banquito.accountcore.enums.AccountingProductType;
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
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class AccountTransactionService {

    private static final ZoneId BANK_ZONE = ZoneId.of("America/Guayaquil");

    private final AccountRepository accountRepository;
    private final AccountTransactionRepository transactionRepository;
    private final TransactionSubtypeRepository transactionSubtypeRepository;
    private final AccountingServiceClient accountingServiceClient;
    private final PartyServiceClient partyServiceClient;
    private final AccountingRulesProperties accountingRules;

    public AccountTransactionService(AccountRepository accountRepository,
                                     AccountTransactionRepository transactionRepository,
                                     TransactionSubtypeRepository transactionSubtypeRepository,
                                     AccountingServiceClient accountingServiceClient,
                                     PartyServiceClient partyServiceClient,
                                     AccountingRulesProperties accountingRules) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.transactionSubtypeRepository = transactionSubtypeRepository;
        this.accountingServiceClient = accountingServiceClient;
        this.partyServiceClient = partyServiceClient;
        this.accountingRules = accountingRules;
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

        LocalDate accountingDate = LocalDate.now(BANK_ZONE);
        AccountTransaction transaction = transactionRepository.save(createTransaction(
                account,
                new TransactionCreationData(
                        request.amount(),
                        TransactionType.CREDITO,
                        TransactionSubtypeCode.DEP_VEN,
                        request.transactionUuid(),
                        accountingDate,
                        account.getAvailableBalance(),
                        descriptionOrDefault(request.reference(), "Teller deposit")
                )
        ));

        accountingServiceClient.postOperation(new AccountingOperationReqDTO(
                request.transactionUuid(),
                AccountingOperationType.TELLER_DEPOSIT,
                getAccountingProductType(account),
                request.amount(),
                null,
                descriptionOrDefault(request.reference(), "Teller deposit account " + account.getId()),
                accountingDate
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

        LocalDate accountingDate = LocalDate.now(BANK_ZONE);
        AccountTransaction transaction = transactionRepository.save(createTransaction(
                account,
                new TransactionCreationData(
                        request.amount(),
                        TransactionType.DEBITO,
                        TransactionSubtypeCode.RET_VEN,
                        request.transactionUuid(),
                        accountingDate,
                        account.getAvailableBalance(),
                        descriptionOrDefault(request.reference(), "Teller withdrawal")
                )
        ));

        accountingServiceClient.postOperation(new AccountingOperationReqDTO(
                request.transactionUuid(),
                AccountingOperationType.TELLER_WITHDRAWAL,
                getAccountingProductType(account),
                request.amount(),
                null,
                descriptionOrDefault(request.reference(), "Teller withdrawal account " + account.getId()),
                accountingDate
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

        LocalDate accountingDate = LocalDate.now(BANK_ZONE);
        AccountTransaction debitTransaction = transactionRepository.save(createTransaction(
                sourceAccount,
                new TransactionCreationData(
                        request.amount(),
                        TransactionType.DEBITO,
                        TransactionSubtypeCode.TRF_P2P_S,
                        request.transactionUuid(),
                        accountingDate,
                        sourceAccount.getAvailableBalance(),
                        descriptionOrDefault(request.reference(), "Internal P2P transfer sent")
                )
        ));
        transactionRepository.save(createTransaction(
                destinationAccount,
                new TransactionCreationData(
                        request.amount(),
                        TransactionType.CREDITO,
                        TransactionSubtypeCode.TRF_P2P_E,
                        request.transactionUuid(),
                        accountingDate,
                        destinationAccount.getAvailableBalance(),
                        descriptionOrDefault(request.reference(), "Internal P2P transfer received")
                )
        ));

        accountingServiceClient.postOperation(new AccountingOperationReqDTO(
                request.transactionUuid(),
                AccountingOperationType.P2P_TRANSFER,
                getAccountingProductType(sourceAccount),
                request.amount(),
                BigDecimal.ZERO,
                descriptionOrDefault(
                        request.reference(),
                        "Internal P2P transfer " + sourceAccount.getAccountNumber()
                                + " to " + destinationAccount.getAccountNumber()),
                accountingDate
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

            Account account = getAccountForUpdate(creditItem.accountNumber());
            validateActiveAccount(account);
            partyServiceClient.validateActiveCustomer(account.getCustomerId());

            credit(account, creditItem.amount());
            accountRepository.save(account);

            LocalDate accountingDate = LocalDate.now(BANK_ZONE);
            transactionRepository.save(createTransaction(
                    account,
                    new TransactionCreationData(
                            creditItem.amount(),
                            TransactionType.CREDITO,
                            TransactionSubtypeCode.PAG_NOM_C,
                            creditItem.transactionUuid(),
                            accountingDate,
                            account.getAvailableBalance(),
                            descriptionOrDefault(creditItem.reference(), "Batch credit " + request.batchId())
                    )
            ));

            accountingServiceClient.postOperation(new AccountingOperationReqDTO(
                    creditItem.transactionUuid(),
                    AccountingOperationType.BATCH_CREDIT,
                    getAccountingProductType(account),
                    creditItem.amount(),
                    BigDecimal.ZERO,
                    descriptionOrDefault(
                            creditItem.reference(),
                            "Batch credit " + request.batchId() + " account " + account.getId()),
                    accountingDate
            ));

            results.add(new BatchCreditResponseDTO.BatchCreditResultDTO(
                    creditItem.accountNumber(),
                    "SUCCESS",
                    creditItem.transactionUuid()
            ));
        }

        return new BatchCreditResponseDTO(request.batchId(), results.size(), 0, results);
    }

    @Transactional
    public CorporateDebitResponseDTO executeCorporateDebit(CorporateDebitReqDTO request) {
        validateIdempotency(request.transactionUuid());

        Account account = getAccountForUpdate(request.accountNumber());
        validateActiveAccount(account);
        partyServiceClient.validateActiveCustomer(account.getCustomerId());

        BigDecimal ivaAmount = request.commissionAmount()
                .multiply(accountingRules.ivaRate())
                .divide(BigDecimal.ONE.add(accountingRules.ivaRate()), 2, RoundingMode.HALF_UP);
        BigDecimal commissionNet = request.commissionAmount().subtract(ivaAmount);
        BigDecimal debitedAmount = request.totalAmount().add(request.commissionAmount());
        validateSufficientBalance(account, debitedAmount);

        debit(account, debitedAmount);
        accountRepository.save(account);

        LocalDate accountingDate = LocalDate.now(BANK_ZONE);
        AccountTransaction transaction = transactionRepository.save(createTransaction(
                account,
                new TransactionCreationData(
                        debitedAmount,
                        TransactionType.DEBITO,
                        TransactionSubtypeCode.DEB_EMP,
                        request.transactionUuid(),
                        accountingDate,
                        account.getAvailableBalance(),
                        "Corporate debit batch " + request.batchId()
                )
        ));

        accountingServiceClient.postOperation(new AccountingOperationReqDTO(
                request.transactionUuid(),
                AccountingOperationType.CORPORATE_DEBIT,
                getAccountingProductType(account),
                request.totalAmount(),
                commissionNet,
                "Corporate debit batch " + request.batchId(),
                accountingDate
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
        LocalDateTime from = LocalDateTime.now(BANK_ZONE).minusDays(1);
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
        if (account.getStatus() != AccountStatus.ACTIVA) {
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

    private AccountTransaction createTransaction(Account account, TransactionCreationData data) {
        AccountTransaction transaction = new AccountTransaction();
        transaction.setAccount(account);
        transaction.setAmount(data.amount());
        transaction.setMovementType(data.transactionType());
        transaction.setTransactionSubtype(getTransactionSubtype(data.transactionSubtype()));
        transaction.setTransactionUuid(data.transactionUuid());
        transaction.setAccountingDate(data.accountingDate());
        transaction.setResultingBalance(data.resultingBalance());
        transaction.setStatus(TransactionStatus.COMPLETADA);
        transaction.setDescription(data.description());
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

    private AccountingProductType getAccountingProductType(Account account) {
        AccountSuperType superType = account.getAccountSubtype().getSuperType();
        return switch (superType) {
            case AHORROS -> AccountingProductType.SAVINGS;
            case CORRIENTE -> AccountingProductType.CHECKING;
        };
    }

    private TransactionSubtype getTransactionSubtype(TransactionSubtypeCode subtypeCode) {
        return transactionSubtypeRepository.findByCode(subtypeCode.name())
                .orElseThrow(() -> new IllegalStateException("Transaction subtype is not configured: " + subtypeCode.name()));
    }

    private String descriptionOrDefault(String description, String defaultDescription) {
        return description == null || description.isBlank() ? defaultDescription : description;
    }

    private record TransactionCreationData(
            BigDecimal amount,
            TransactionType transactionType,
            TransactionSubtypeCode transactionSubtype,
            String transactionUuid,
            LocalDate accountingDate,
            BigDecimal resultingBalance,
            String description
    ) {}
}

