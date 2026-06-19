package ec.edu.espe.banquito.accountcore.dto;

import ec.edu.espe.banquito.accountcore.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * RF-03: Respuesta de la devolución corporativa.
 */
public record CorporateRefundResponseDTO(
        String transactionUuid,
        BigDecimal refundedAmount,
        TransactionStatus status,
        LocalDate accountingDate
) {}
