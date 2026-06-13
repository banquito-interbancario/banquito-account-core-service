package ec.edu.espe.banquito.accountcore.dto;

import ec.edu.espe.banquito.accountcore.enums.AccountingOperationType;
import ec.edu.espe.banquito.accountcore.enums.AccountingProductType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AccountingOperationReqDTO(
        String operationUuid,
        AccountingOperationType operationType,
        AccountingProductType accountProductType,
        BigDecimal amount,
        BigDecimal commissionAmount,
        String reference,
        LocalDate accountingDate
) {}
