package ec.edu.espe.banquito.accountcore.controller;

import ec.edu.espe.banquito.accountcore.dto.DailyTransactionsFileResponseDTO;
import ec.edu.espe.banquito.accountcore.service.DailyTransactionsFileService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v2/eod")
public class EodController {

    private final DailyTransactionsFileService dailyTransactionsFileService;

    public EodController(DailyTransactionsFileService dailyTransactionsFileService) {
        this.dailyTransactionsFileService = dailyTransactionsFileService;
    }

    @GetMapping("/daily-transactions-file")
    public ResponseEntity<DailyTransactionsFileResponseDTO> generateDailyTransactionsFile(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(dailyTransactionsFileService.generateDailyTransactionsFile(date));
    }
}
