package ec.edu.espe.banquito.accountcore.enums;

public enum AccountSuperType {
    SAVINGS,
    CHECKING;

    public static AccountSuperType fromDatabaseValue(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "AHORRO", "AHORROS", "SAVINGS" -> SAVINGS;
            case "CORRIENTE", "CHECKING" -> CHECKING;
            default -> AccountSuperType.valueOf(value);
        };
    }
}
