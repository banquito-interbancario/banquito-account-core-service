package ec.edu.espe.banquito.accountcore.service;

import ec.edu.espe.banquito.accountcore.dto.DailyTransactionsFileResponseDTO;
import ec.edu.espe.banquito.accountcore.enums.TransactionType;
import ec.edu.espe.banquito.accountcore.model.AccountTransaction;
import ec.edu.espe.banquito.accountcore.repository.AccountTransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class DailyTransactionsFileService {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String BOM = new String(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, StandardCharsets.UTF_8);

    private final AccountTransactionRepository transactionRepository;
    private final Path reportsDir;

    public DailyTransactionsFileService(AccountTransactionRepository transactionRepository,
                                         @Value("${app.eod.reports-dir:./reports}") String reportsDir) {
        this.transactionRepository = transactionRepository;
        this.reportsDir = Path.of(reportsDir);
    }

    @Transactional(readOnly = true)
    public DailyTransactionsFileResponseDTO generateDailyTransactionsFile(LocalDate accountingDate) {
        List<AccountTransaction> transactions = transactionRepository.findAllByAccountingDate(accountingDate);

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        for (AccountTransaction transaction : transactions) {
            if (transaction.getMovementType() == TransactionType.DEBITO) {
                totalDebits = totalDebits.add(transaction.getAmount());
            } else {
                totalCredits = totalCredits.add(transaction.getAmount());
            }
        }

        String filePath = writeCsv(accountingDate, transactions);

        return new DailyTransactionsFileResponseDTO(
                accountingDate, transactions.size(), totalDebits, totalCredits, filePath);
    }

    private String writeCsv(LocalDate date, List<AccountTransaction> transactions) {
        List<String> rows = new ArrayList<>();
        rows.add("ID Transaccion,UUID,Cuenta,Tipo Movimiento,Subtipo,Monto,Saldo Resultante,"
                + "Descripcion,Fecha Real,Fecha Contable,Estado");

        for (AccountTransaction transaction : transactions) {
            String subtypeCode = transaction.getTransactionSubtype() != null
                    ? transaction.getTransactionSubtype().getCode()
                    : "";
            rows.add(String.join(",",
                    String.valueOf(transaction.getId()),
                    transaction.getTransactionUuid(),
                    transaction.getAccount().getAccountNumber(),
                    transaction.getMovementType().name(),
                    subtypeCode,
                    transaction.getAmount().toPlainString(),
                    transaction.getResultingBalance().toPlainString(),
                    "\"" + transaction.getDescription() + "\"",
                    transaction.getTransactionDate().toString(),
                    transaction.getAccountingDate().toString(),
                    transaction.getStatus().name()));
        }

        try {
            Files.createDirectories(reportsDir);
            Path file = reportsDir.resolve("transacciones_dia_" + date.format(FILE_DATE) + ".csv");
            String content = BOM + String.join("\r\n", rows) + "\r\n";
            Files.writeString(file, content, StandardCharsets.UTF_8);
            return file.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo escribir el archivo de transacciones del dia", e);
        }
    }
}
