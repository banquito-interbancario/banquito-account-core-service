package ec.edu.espe.banquito.accountcore.service;

import ec.edu.espe.banquito.accountcore.dto.StaffLoginRequestDTO;
import ec.edu.espe.banquito.accountcore.dto.StaffLoginResponseDTO;
import ec.edu.espe.banquito.accountcore.exception.InvalidCredentialsException;
import ec.edu.espe.banquito.accountcore.model.CoreUser;
import ec.edu.espe.banquito.accountcore.repository.CoreUserRepository;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
public class CoreUserAuthService {

    private static final String ACTIVE_STATUS = "ACTIVO";
    private static final String TELLER_ROLE = "CAJERO";
    private static final String OPERATOR_ROLE = "OPERADOR";
    private static final ZoneId BANK_ZONE = ZoneId.of("America/Guayaquil");

    private final CoreUserRepository coreUserRepository;

    public CoreUserAuthService(CoreUserRepository coreUserRepository) {
        this.coreUserRepository = coreUserRepository;
    }

    @Transactional
    public StaffLoginResponseDTO loginStaff(StaffLoginRequestDTO request) {
        log.info("Intento de login para usuario: {}", request.username());
        
        CoreUser coreUser = coreUserRepository.findByUsername(request.username())
                .orElseThrow(() -> {
                    log.error("Usuario no encontrado en la base de datos: {}", request.username());
                    return new InvalidCredentialsException();
                });

        log.info("Usuario encontrado: {}, Rol: {}, Estado: {}", coreUser.getUsername(), coreUser.getRole(), coreUser.getStatus());

        boolean passwordMatch = BCrypt.checkpw(request.password(), coreUser.getPasswordHash());
        log.info("¿Contraseña coincide?: {}", passwordMatch);

        if (!isValidStaff(coreUser) || !passwordMatch) {
            log.error("Fallo de validación: isValidStaff={}, passwordMatch={}", isValidStaff(coreUser), passwordMatch);
            throw new InvalidCredentialsException();
        }

        coreUser.setLastLogin(LocalDateTime.now(BANK_ZONE));
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
        return (TELLER_ROLE.equals(coreUser.getRole()) || OPERATOR_ROLE.equals(coreUser.getRole())) 
               && ACTIVE_STATUS.equals(coreUser.getStatus());
    }

    private Integer resolveBranchId(CoreUser coreUser) {
        if (coreUser.getBranchId() != null) {
            return coreUser.getBranchId();
        }
        String username = coreUser.getUsername().toLowerCase();
        if (username.contains("norte") || username.contains("anahy") || username.contains("bryan")) return 1;
        if (username.contains("sur") || username.contains("oscar") || username.contains("paul")) return 2;
        if (username.contains("centro") || username.contains("anthony") || username.contains("johan")) return 3;
        if (username.contains("valles") || username.contains("santiago") || username.contains("alan")) return 4;
        return 1;
    }

    private String resolveBranchCode(CoreUser coreUser) {
        if (coreUser.getBranchCode() != null) {
            return coreUser.getBranchCode();
        }
        String username = coreUser.getUsername().toLowerCase();
        if (username.contains("norte") || username.contains("anahy") || username.contains("bryan")) return "NORTE";
        if (username.contains("sur") || username.contains("oscar") || username.contains("paul")) return "SUR";
        if (username.contains("centro") || username.contains("anthony") || username.contains("johan")) return "CENTRO";
        if (username.contains("valles") || username.contains("santiago") || username.contains("alan")) return "VALLES";
        return "MATRIZ";
    }
}
