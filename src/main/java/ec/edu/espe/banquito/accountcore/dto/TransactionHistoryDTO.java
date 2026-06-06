package ec.edu.espe.banquito.accountcore.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TransactionHistoryDTO(
        List<TransactionHistoryItemDTO> content,
        long totalElements,
        int page
) {
    public record TransactionHistoryItemDTO(
            String transactionUuid,
            String movementType,
            BigDecimal amount,
            BigDecimal resultingBalance,
            LocalDateTime transactionDate,
            LocalDate accountingDate,
            String description
    ) {}
}
