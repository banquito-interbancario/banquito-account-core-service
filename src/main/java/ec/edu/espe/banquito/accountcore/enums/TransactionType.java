package ec.edu.espe.banquito.accountcore.enums;

public enum TransactionType {
    DEBIT,
    CREDIT;

    public static TransactionType fromDatabaseValue(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "DEBITO", "DEBIT" -> DEBIT;
            case "CREDITO", "CREDIT" -> CREDIT;
            default -> TransactionType.valueOf(value);
        };
    }
}
