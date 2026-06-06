package ec.edu.espe.banquito.accountcore.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CorporateDebitReqDTO(
        @NotNull(message = "Account ID is required")
        Long accountId,

        @NotNull(message = "Total amount is required")
        @Positive(message = "Total amount must be greater than zero")
        BigDecimal totalAmount,

        @NotNull(message = "Commission amount is required")
        @PositiveOrZero(message = "Commission amount must be zero or greater")
        BigDecimal commissionAmount,

        @NotNull(message = "Batch ID is required")
        String batchId,

        @NotNull(message = "Transaction UUID is required")
        String transactionUuid
) {}
