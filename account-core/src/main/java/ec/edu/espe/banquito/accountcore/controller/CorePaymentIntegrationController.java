package ec.edu.espe.banquito.accountcore.controller;

import ec.edu.espe.banquito.accountcore.dto.CorporateDebitReqDTO;
import ec.edu.espe.banquito.accountcore.dto.TellerTransactionReqDTO;
import ec.edu.espe.banquito.accountcore.service.AccountTransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v2/payments")
public class CorePaymentIntegrationController {

    private final AccountTransactionService transactionService;

    public CorePaymentIntegrationController(AccountTransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/corporate-debit")
    public ResponseEntity<?> corporateDebit(@RequestBody CorporateDebitReqDTO dto) {
        try {
            transactionService.executeCorporateDebit(dto);
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Débito corporativo y comisiones liquidadas exitosamente."));
        } catch (IllegalStateException e) {
            if ("TRANSACTION_UUID_DUPLICATED".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Operación duplicada detectada para el mismo día."));
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // Si el servicio de contabilidad está caído o da timeout, responde con un 503
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Error en el sistema contable de respaldo. Transacción revertida de forma segura.", "details", e.getMessage()));
        }
    }

    @PostMapping("/teller/deposit")
    public ResponseEntity<?> tellerDeposit(@RequestBody TellerTransactionReqDTO dto) {
        try {
            transactionService.executeDeposit(dto);
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "uuid", dto.transactionUuid()));
        } catch (IllegalStateException e) {
            if ("TRANSACTION_UUID_DUPLICATED".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Transacción duplicada."));
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Error temporal de comunicación con el libro mayor contable.", "details", e.getMessage()));
        }
    }
}