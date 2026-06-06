package ec.edu.espe.banquito.accountcore.exception;

public class DuplicateTransactionException extends RuntimeException {

    public DuplicateTransactionException(String transactionUuid) {
        super("Duplicate transaction: " + transactionUuid);
    }
}
