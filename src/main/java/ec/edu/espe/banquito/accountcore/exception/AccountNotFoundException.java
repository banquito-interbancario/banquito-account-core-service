package ec.edu.espe.banquito.accountcore.exception;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String accountNumber) {
        super("Account not found: " + accountNumber);
    }

    public AccountNotFoundException(Long accountId) {
        super("Account not found: " + accountId);
    }
}
