package ec.edu.espe.banquito.accountcore.dto;

public record StaffLoginResponseDTO(
        Integer id,
        String username,
        String name,
        String role,
        Integer branchId,
        String branchCode,
        String status
) {}
