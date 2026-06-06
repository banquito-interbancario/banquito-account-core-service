package ec.edu.espe.banquito.accountcore.enums;

public enum AccountStatus {
    ACTIVE,
    INACTIVE,
    BLOCKED,
    SUSPENDED;

    public static AccountStatus fromDatabaseValue(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "ACTIVA", "ACTIVO", "ACTIVE" -> ACTIVE;
            case "INACTIVA", "INACTIVO", "INACTIVE" -> INACTIVE;
            case "BLOQUEADA", "BLOQUEADO", "BLOCKED" -> BLOCKED;
            case "SUSPENDIDA", "SUSPENDIDO", "SUSPENDED" -> SUSPENDED;
            default -> AccountStatus.valueOf(value);
        };
    }
}
