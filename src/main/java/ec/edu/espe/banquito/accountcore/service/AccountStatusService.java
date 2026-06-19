package ec.edu.espe.banquito.accountcore.service;

import ec.edu.espe.banquito.accountcore.client.NotificationGrpcClient;
import ec.edu.espe.banquito.accountcore.client.PartyServiceClient;
import ec.edu.espe.banquito.accountcore.enums.AccountStatus;
import ec.edu.espe.banquito.accountcore.exception.AccountNotFoundException;
import ec.edu.espe.banquito.accountcore.model.Account;
import ec.edu.espe.banquito.accountcore.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

@Service
public class AccountStatusService {

    private final AccountRepository accountRepository;
    private final PartyServiceClient partyServiceClient;
    private final NotificationGrpcClient notificationGrpcClient;

    public AccountStatusService(AccountRepository accountRepository,
                                 PartyServiceClient partyServiceClient,
                                 NotificationGrpcClient notificationGrpcClient) {
        this.accountRepository = accountRepository;
        this.partyServiceClient = partyServiceClient;
        this.notificationGrpcClient = notificationGrpcClient;
    }

    @Transactional
    public AccountStatus changeStatus(String accountNumber, AccountStatus newStatus) {
        Account account = this.accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        account.setStatus(newStatus);
        AccountStatus savedStatus = this.accountRepository.save(account).getStatus();

        notifyStatusChange(account, savedStatus);

        return savedStatus;
    }

    private void notifyStatusChange(Account account, AccountStatus newStatus) {
        try {
            String email = this.partyServiceClient.getCustomerEmail(account.getCustomerId());
            if (email == null || email.isBlank()) {
                return;
            }
            String customerName = this.partyServiceClient.getHolderNameByAccount(account.getAccountNumber());

            this.notificationGrpcClient.sendNotification(
                    email,
                    "BanQuito - Cambio de estado en tu cuenta " + account.getAccountNumber(),
                    "ACCOUNT_STATUS_CHANGED",
                    Map.of(
                            "customerName", customerName != null ? customerName : "",
                            "accountNumber", account.getAccountNumber(),
                            "newStatus", newStatus.name(),
                            "date", LocalDate.now().toString()
                    )
            );
        } catch (Exception ignored) {
            // No bloquear el cambio de estado si la notificación falla.
        }
    }
}
