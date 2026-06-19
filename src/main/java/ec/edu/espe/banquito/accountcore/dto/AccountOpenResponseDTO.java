package ec.edu.espe.banquito.accountcore.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AccountOpenResponseDTO(
        Long accountId,
        String accountNumber,
        String branchCode,
        String branchName,
        Long customerId,
        String accountSubtypeName,
        BigDecimal initialDeposit,
        String status,
        LocalDate openingDate
) {}
