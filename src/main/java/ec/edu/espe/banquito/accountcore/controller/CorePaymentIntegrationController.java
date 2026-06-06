package ec.edu.espe.banquito.accountcore.controller;

import ec.edu.espe.banquito.accountcore.dto.BatchCreditReqDTO;
import ec.edu.espe.banquito.accountcore.dto.BatchCreditResponseDTO;
import ec.edu.espe.banquito.accountcore.dto.CorporateDebitReqDTO;
import ec.edu.espe.banquito.accountcore.dto.CorporateDebitResponseDTO;
import ec.edu.espe.banquito.accountcore.service.AccountTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/payments")
@Tag(name = "Payments", description = "REST integration endpoints consumed by Switch/Routing.")
public class CorePaymentIntegrationController {

    private final AccountTransactionService transactionService;

    public CorePaymentIntegrationController(AccountTransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/batch-credit")
    @Operation(summary = "Process batch credit", description = "Credits multiple on-us accounts from a Switch/Routing batch.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Batch processed",
                    content = @Content(schema = @Schema(implementation = BatchCreditResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request, inactive account or business rule failure"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "409", description = "Duplicated transaction UUID"),
            @ApiResponse(responseCode = "503", description = "Required core gRPC service unavailable")
    })
    public ResponseEntity<BatchCreditResponseDTO> batchCredit(@Valid @RequestBody BatchCreditReqDTO request) {
        return ResponseEntity.ok(transactionService.executeBatchCredit(request));
    }

    @PostMapping("/corporate-debit")
    @Operation(summary = "Process corporate debit", description = "Debits a corporate account for a payroll or massive-payment batch and commission.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Corporate debit processed",
                    content = @Content(schema = @Schema(implementation = CorporateDebitResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request, inactive account or insufficient balance"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "409", description = "Duplicated transaction UUID"),
            @ApiResponse(responseCode = "503", description = "Required core gRPC service unavailable")
    })
    public ResponseEntity<CorporateDebitResponseDTO> corporateDebit(@Valid @RequestBody CorporateDebitReqDTO request) {
        return ResponseEntity.ok(transactionService.executeCorporateDebit(request));
    }
}
