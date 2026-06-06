package ec.edu.espe.banquito.accountcore.dto;

import ec.edu.espe.banquito.accountcore.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CorporateDebitResponseDTO(
        String transactionId,
        BigDecimal debitedAmount,
        BigDecimal commissionNet,
        BigDecimal ivaAmount,
        TransactionStatus status,
        LocalDate accountingDate
) {}
