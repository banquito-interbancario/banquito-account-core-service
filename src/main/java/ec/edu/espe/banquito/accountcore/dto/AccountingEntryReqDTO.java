package ec.edu.espe.banquito.accountcore.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AccountingEntryReqDTO(
        String entryUuid,
        String description,
        LocalDate entryDate,
        List<JournalLineDTO> lines
) {
    public record JournalLineDTO(
            String accountCode,
            String movementType,
            BigDecimal amount,
            String reference
    ) {}
}
