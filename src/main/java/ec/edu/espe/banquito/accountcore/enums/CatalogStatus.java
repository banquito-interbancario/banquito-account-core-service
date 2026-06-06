package ec.edu.espe.banquito.accountcore.enums;

public enum CatalogStatus {
    ACTIVE,
    INACTIVE;

    public static CatalogStatus fromDatabaseValue(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "ACTIVO", "ACTIVE" -> ACTIVE;
            case "INACTIVO", "INACTIVE" -> INACTIVE;
            default -> CatalogStatus.valueOf(value);
        };
    }
}
