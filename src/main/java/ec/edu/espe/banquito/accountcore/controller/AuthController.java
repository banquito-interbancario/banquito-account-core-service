package ec.edu.espe.banquito.accountcore.controller;

import ec.edu.espe.banquito.accountcore.dto.StaffLoginRequestDTO;
import ec.edu.espe.banquito.accountcore.dto.StaffLoginResponseDTO;
import ec.edu.espe.banquito.accountcore.service.CoreUserAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/auth")
@Tag(name = "Authentication", description = "Staff authentication for teller operations.")
@org.springframework.web.bind.annotation.CrossOrigin(origins = "*", allowedHeaders = "*")
public class AuthController {

    private final CoreUserAuthService coreUserAuthService;

    public AuthController(CoreUserAuthService coreUserAuthService) {
        this.coreUserAuthService = coreUserAuthService;
    }

    @PostMapping("/login/staff")
    @Operation(summary = "Login staff user", description = "Authenticates an active teller from CORE_USER.")
    @ApiResponse(responseCode = "200", description = "Staff authenticated",
            content = @Content(schema = @Schema(implementation = StaffLoginResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Invalid credentials or inactive/non-teller user")
    public ResponseEntity<StaffLoginResponseDTO> loginStaff(@Valid @RequestBody StaffLoginRequestDTO request) {
        return ResponseEntity.ok(coreUserAuthService.loginStaff(request));
    }
}
