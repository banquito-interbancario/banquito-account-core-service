package ec.edu.espe.banquito.accountcore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CorporateDebitReqDTO(
        @NotBlank(message = "Account number is required")
        String accountNumber,

        @NotNull(message = "Total amount is required")
        @PositiveOrZero(message = "Total amount must be zero or greater")
        BigDecimal totalAmount,

        @NotNull(message = "Commission amount is required")
        @PositiveOrZero(message = "Commission amount must be zero or greater")
        BigDecimal commissionAmount,

        @NotNull(message = "Batch ID is required")
        String batchId,

        @NotNull(message = "Transaction UUID is required")
        String transactionUuid
) {}
