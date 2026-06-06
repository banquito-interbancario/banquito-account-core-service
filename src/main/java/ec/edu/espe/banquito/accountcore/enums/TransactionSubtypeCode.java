package ec.edu.espe.banquito.accountcore.enums;

public enum TransactionSubtypeCode {
    TELLER_DEPOSIT("DEP_VEN"),
    TELLER_WITHDRAWAL("RET_VEN"),
    BATCH_CREDIT("PAG_NOM_C"),
    CORPORATE_DEBIT("DEB_EMP"),
    P2P_OUT("TRF_P2P_S"),
    P2P_IN("TRF_P2P_E");

    private final String databaseCode;

    TransactionSubtypeCode(String databaseCode) {
        this.databaseCode = databaseCode;
    }

    public String databaseCode() {
        return databaseCode;
    }

    public static TransactionSubtypeCode fromDatabaseValue(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "DEP", "DEP_VEN", "TELLER_DEPOSIT" -> TELLER_DEPOSIT;
            case "RET", "RET_VEN", "TELLER_WITHDRAWAL" -> TELLER_WITHDRAWAL;
            case "PAG_NOM_C", "BATCH_CREDIT" -> BATCH_CREDIT;
            case "DEB_EMP", "DEB_CORP", "CORPORATE_DEBIT" -> CORPORATE_DEBIT;
            case "TRF_P2P_S", "P2P_OUT" -> P2P_OUT;
            case "TRF_P2P_E", "P2P_IN" -> P2P_IN;
            default -> TransactionSubtypeCode.valueOf(value);
        };
    }
}
