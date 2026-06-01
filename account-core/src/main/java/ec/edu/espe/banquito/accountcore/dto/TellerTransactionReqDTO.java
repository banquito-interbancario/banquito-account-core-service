package ec.edu.espe.banquito.accountcore.dto;

import java.annotation.Nonnull;
import java.math.BigDecimal;

public record TellerTransactionReqDTO(
        @Nonnull String accountNumber,
        @Nonnull BigDecimal amount,
        @Nonnull String transactionUuid,
        @Nonnull String tellerId
) {}