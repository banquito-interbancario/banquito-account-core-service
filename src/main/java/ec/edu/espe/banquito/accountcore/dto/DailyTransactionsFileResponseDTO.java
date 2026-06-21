package ec.edu.espe.banquito.accountcore.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyTransactionsFileResponseDTO(
        LocalDate accountingDate,
        int totalTransactions,
        BigDecimal totalDebits,
        BigDecimal totalCredits,
        String filePath
) {
}
