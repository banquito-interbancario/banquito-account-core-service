package ec.edu.espe.banquito.accountcore.dto;

import java.math.BigDecimal;
import java.util.List;

public record AccountingEntryReqDTO(
        String entryUuid,
        String description,
        List<JournalLineDTO> lines
) {
    public record JournalLineDTO(
            String accountCode,
            String movementType, // "DEBIT" o "CREDIT"
            BigDecimal amount,
            String reference
    ) {}
}