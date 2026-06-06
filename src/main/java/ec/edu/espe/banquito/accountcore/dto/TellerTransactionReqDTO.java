package ec.edu.espe.banquito.accountcore.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TellerTransactionReqDTO(
        @NotNull(message = "Account ID is required")
        Long accountId,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotNull(message = "Teller ID is required")
        Long tellerId,

        @NotNull(message = "Branch ID is required")
        Long branchId,

        @NotNull(message = "Transaction UUID is required")
        String transactionUuid,

        String reference
) {}
