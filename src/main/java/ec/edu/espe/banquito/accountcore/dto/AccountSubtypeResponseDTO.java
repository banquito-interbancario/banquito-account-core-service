package ec.edu.espe.banquito.accountcore.dto;

public record AccountSubtypeResponseDTO(
    Integer id,
    String name,
    String description,
    String type
) {}
