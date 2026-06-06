package ec.edu.espe.banquito.accountcore.dto;

public record HealthResponseDTO(
        String status,
        String service,
        String version
) {}
