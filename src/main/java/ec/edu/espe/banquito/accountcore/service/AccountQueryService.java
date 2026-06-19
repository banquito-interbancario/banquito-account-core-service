package ec.edu.espe.banquito.accountcore.service;

import ec.edu.espe.banquito.accountcore.client.PartyServiceClient;
import ec.edu.espe.banquito.accountcore.dto.AccountDetailResponseDTO;
import ec.edu.espe.banquito.accountcore.dto.FavoriteAccountResponseDTO;
import ec.edu.espe.banquito.accountcore.exception.AccountNotFoundException;
import ec.edu.espe.banquito.accountcore.exception.FavoriteAccountNotFoundException;
import ec.edu.espe.banquito.accountcore.model.Account;
import ec.edu.espe.banquito.accountcore.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountQueryService {

    private static final String CURRENCY = "USD";

    private final AccountRepository accountRepository;
    private final PartyServiceClient partyServiceClient;

    public AccountQueryService(AccountRepository accountRepository, PartyServiceClient partyServiceClient) {
        this.accountRepository = accountRepository;
        this.partyServiceClient = partyServiceClient;
    }

    @Transactional(readOnly = true)
    public Account resolveAccount(String accountIdOrNumber) {
        return findAccount(accountIdOrNumber);
    }

    @Transactional(readOnly = true)
    public AccountDetailResponseDTO getAccountDetail(String accountIdOrNumber) {
        Account account = findAccount(accountIdOrNumber);

        String customerFullName = null;
        try {
            customerFullName = this.partyServiceClient.getHolderNameByAccount(account.getAccountNumber());
        } catch (Exception ignored) {
            // party-service unreachable or customer not found; leave name blank
        }

        String branchName = null;
        try {
            branchName = this.partyServiceClient.getBranch(account.getBranchId()).getName();
        } catch (Exception ignored) {
            // party-service unreachable or branch not found; leave name blank
        }

        return new AccountDetailResponseDTO(
                account.getId(),
                account.getAccountNumber(),
                account.getCustomerId(),
                customerFullName,
                account.getAccountSubtype().getDescription() != null
                        ? account.getAccountSubtype().getDescription()
                        : account.getAccountSubtype().getName(),
                account.getBranchId(),
                branchName,
                account.getAvailableBalance(),
                account.getAccountingBalance(),
                account.getStatus(),
                account.getOpeningDate()
        );
    }

    private Account findAccount(String accountIdOrNumber) {
        if (accountIdOrNumber.matches("\\d+") && accountIdOrNumber.length() <= 6) {
            return this.accountRepository.findById(Long.valueOf(accountIdOrNumber))
                    .orElseThrow(() -> new AccountNotFoundException(accountIdOrNumber));
        }
        return this.accountRepository.findByAccountNumber(accountIdOrNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountIdOrNumber));
    }

    @Transactional(readOnly = true)
    public FavoriteAccountResponseDTO getFavoriteAccount(Long customerId) {
        if (customerId == null || customerId <= 0) {
            throw new IllegalArgumentException("Customer ID must be greater than zero");
        }

        Account account = accountRepository.findFirstByCustomerIdAndFavoriteTrueOrderByAccountNumberAsc(customerId)
                .orElseThrow(() -> new FavoriteAccountNotFoundException(customerId));

        return new FavoriteAccountResponseDTO(
                account.getId(),
                account.getAccountNumber(),
                account.getCustomerId(),
                account.getStatus(),
                account.getAvailableBalance(),
                account.getAccountingBalance(),
                CURRENCY,
                Boolean.TRUE.equals(account.getFavorite())
        );
    }
}
