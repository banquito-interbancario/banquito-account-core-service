package ec.edu.espe.banquito.accountcore.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransferP2PReqDTO(
        @NotNull(message = "Origin account ID is required")
        Long originAccountId,

        @NotNull(message = "Destination account number is required")
        String destinationAccountNumber,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotNull(message = "Transaction UUID is required")
        String transactionUuid,

        String reference
) {}
