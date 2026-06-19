package ec.edu.espe.banquito.accountcore.enums;

public enum AccountingOperationType {
    TELLER_DEPOSIT,
    TELLER_WITHDRAWAL,
    P2P_TRANSFER,
    BATCH_CREDIT,
    CORPORATE_DEBIT,
    CORPORATE_REFUND  // RF-03: Devolución de monto rechazado a cuenta corporativa
}
