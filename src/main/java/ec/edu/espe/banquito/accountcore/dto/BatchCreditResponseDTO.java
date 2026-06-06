package ec.edu.espe.banquito.accountcore.dto;

import java.util.List;

public record BatchCreditResponseDTO(
        String batchId,
        int processed,
        int failed,
        List<BatchCreditResultDTO> results
) {
    public record BatchCreditResultDTO(
            Long accountId,
            String status,
            String transactionId
    ) {}
}
