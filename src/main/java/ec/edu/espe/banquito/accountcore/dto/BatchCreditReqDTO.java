package ec.edu.espe.banquito.accountcore.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record BatchCreditReqDTO(
        @NotNull(message = "Batch ID is required")
        String batchId,

        @NotEmpty(message = "Credits are required")
        List<@Valid CreditItemDTO> credits
) {
    public record CreditItemDTO(
            @NotNull(message = "Account ID is required")
            Long accountId,

            @NotNull(message = "Amount is required")
            @Positive(message = "Amount must be greater than zero")
            BigDecimal amount,

            String reference,

            @NotNull(message = "Transaction UUID is required")
            String transactionUuid
    ) {}
}
