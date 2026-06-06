package ec.edu.espe.banquito.accountcore.service;

import ec.edu.espe.banquito.accountcore.dto.StaffLoginRequestDTO;
import ec.edu.espe.banquito.accountcore.dto.StaffLoginResponseDTO;
import ec.edu.espe.banquito.accountcore.exception.InvalidCredentialsException;
import ec.edu.espe.banquito.accountcore.model.CoreUser;
import ec.edu.espe.banquito.accountcore.repository.CoreUserRepository;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class CoreUserAuthService {

    private static final String ACTIVE_STATUS = "ACTIVO";
    private static final String TELLER_ROLE = "CAJERO";

    private final CoreUserRepository coreUserRepository;

    public CoreUserAuthService(CoreUserRepository coreUserRepository) {
        this.coreUserRepository = coreUserRepository;
    }

    @Transactional
    public StaffLoginResponseDTO loginStaff(StaffLoginRequestDTO request) {
        CoreUser coreUser = coreUserRepository.findByUsername(request.username())
                .orElseThrow(InvalidCredentialsException::new);

        if (!isValidStaff(coreUser) || !BCrypt.checkpw(request.password(), coreUser.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        coreUser.setLastLogin(LocalDateTime.now());
        coreUserRepository.save(coreUser);

        return new StaffLoginResponseDTO(
                coreUser.getId(),
                coreUser.getUsername(),
                coreUser.getFullName(),
                coreUser.getRole(),
                resolveBranchId(coreUser),
                resolveBranchCode(coreUser),
                coreUser.getStatus()
        );
    }

    private boolean isValidStaff(CoreUser coreUser) {
        return TELLER_ROLE.equals(coreUser.getRole()) && ACTIVE_STATUS.equals(coreUser.getStatus());
    }

    private Integer resolveBranchId(CoreUser coreUser) {
        if (coreUser.getBranchId() != null) {
            return coreUser.getBranchId();
        }
        return switch (coreUser.getUsername()) {
            case "cajero.norte" -> 1;
            case "cajero.sur" -> 2;
            case "cajero.centro" -> 3;
            case "cajero.valles" -> 4;
            default -> null;
        };
    }

    private String resolveBranchCode(CoreUser coreUser) {
        if (coreUser.getBranchCode() != null) {
            return coreUser.getBranchCode();
        }
        return switch (coreUser.getUsername()) {
            case "cajero.norte" -> "NORTE";
            case "cajero.sur" -> "SUR";
            case "cajero.centro" -> "CENTRO";
            case "cajero.valles" -> "VALLES";
            default -> null;
        };
    }
}
