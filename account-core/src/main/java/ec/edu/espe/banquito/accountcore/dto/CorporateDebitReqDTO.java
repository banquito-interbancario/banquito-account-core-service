package ec.edu.espe.banquito.accountcore.dto;

import java.annotation.Nonnull;
import java.math.BigDecimal;

public record CorporateDebitReqDTO(
        @Nonnull String accountId,
        @Nonnull BigDecimal totalAmount,
        @Nonnull BigDecimal commissionAmount,
        @Nonnull String transactionUuid
) {}