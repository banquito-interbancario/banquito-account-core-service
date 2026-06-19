package ec.edu.espe.banquito.accountcore.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record AccountOpenReqDTO(
        @NotNull Long customerId,
        @NotNull Integer branchId,
        @NotNull Integer accountSubtypeId,
        @PositiveOrZero BigDecimal initialDeposit
) {}
