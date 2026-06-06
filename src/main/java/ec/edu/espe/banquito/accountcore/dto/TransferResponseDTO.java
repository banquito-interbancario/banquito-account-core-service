package ec.edu.espe.banquito.accountcore.dto;

import ec.edu.espe.banquito.accountcore.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransferResponseDTO(
        String transactionId,
        BigDecimal originNewBalance,
        String destinationAccountNumber,
        String destinationHolderName,
        TransactionStatus status,
        LocalDate accountingDate
) {}
