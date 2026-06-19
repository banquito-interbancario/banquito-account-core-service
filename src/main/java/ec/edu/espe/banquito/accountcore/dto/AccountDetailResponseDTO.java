package ec.edu.espe.banquito.accountcore.dto;

import ec.edu.espe.banquito.accountcore.enums.AccountStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AccountDetailResponseDTO(
        Long accountId,
        String accountNumber,
        Long customerId,
        String customerFullName,
        String accountSubtypeDescription,
        Integer branchId,
        String branchName,
        BigDecimal availableBalance,
        BigDecimal accountingBalance,
        AccountStatus status,
        LocalDate openingDate
) {}
