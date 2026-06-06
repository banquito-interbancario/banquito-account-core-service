package ec.edu.espe.banquito.accountcore.exception;

public class InactiveAccountException extends RuntimeException {

    public InactiveAccountException(String accountNumber) {
        super("Account is not active: " + accountNumber);
    }
}
