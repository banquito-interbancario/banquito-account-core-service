package ec.edu.espe.banquito.accountcore.service;

import ec.edu.espe.banquito.accountcore.client.PartyServiceClient;
import ec.edu.espe.banquito.accountcore.dto.AccountOpenReqDTO;
import ec.edu.espe.banquito.accountcore.dto.AccountOpenResponseDTO;
import ec.edu.espe.banquito.accountcore.enums.AccountStatus;
import ec.edu.espe.banquito.accountcore.grpc.party.BranchResponse;
import ec.edu.espe.banquito.accountcore.model.Account;
import ec.edu.espe.banquito.accountcore.model.AccountSubtype;
import ec.edu.espe.banquito.accountcore.repository.AccountRepository;
import ec.edu.espe.banquito.accountcore.repository.AccountSubtypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class AccountOpenService {

    private final AccountRepository accountRepository;
    private final AccountSubtypeRepository accountSubtypeRepository;
    private final PartyServiceClient partyServiceClient;

    public AccountOpenService(AccountRepository accountRepository,
                              AccountSubtypeRepository accountSubtypeRepository,
                              PartyServiceClient partyServiceClient) {
        this.accountRepository = accountRepository;
        this.accountSubtypeRepository = accountSubtypeRepository;
        this.partyServiceClient = partyServiceClient;
    }

    @Transactional
    public AccountOpenResponseDTO openAccount(AccountOpenReqDTO req) {
        AccountSubtype subtype = accountSubtypeRepository.findById(req.accountSubtypeId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subtipo de cuenta no encontrado: " + req.accountSubtypeId()));

        BranchResponse branch;
        try {
            branch = this.partyServiceClient.getBranch(req.branchId());
        } catch (Exception exception) {
            throw new IllegalArgumentException("Sucursal no encontrada: " + req.branchId(), exception);
        }

        String branchCode = branch.getBranchCode();
        String accountNumber = generateAccountNumber(branchCode);

        BigDecimal deposit = req.initialDeposit() != null ? req.initialDeposit() : BigDecimal.ZERO;

        Account account = new Account();
        account.setCustomerId(req.customerId());
        account.setBranchId(req.branchId());
        account.setAccountSubtype(subtype);
        account.setAccountNumber(accountNumber);
        account.setAvailableBalance(deposit);
        account.setAccountingBalance(deposit);
        account.setStatus(AccountStatus.ACTIVA);
        account.setFavorite(false);

        Account saved = accountRepository.save(account);

        return new AccountOpenResponseDTO(
                saved.getId(),
                saved.getAccountNumber(),
                branchCode,
                branch.getName(),
                saved.getCustomerId(),
                subtype.getName(),
                deposit,
                saved.getStatus().name(),
                LocalDate.now()
        );
    }

    private String generateAccountNumber(String branchCode) {
        return accountRepository
                .findTopByAccountNumberStartingWithOrderByAccountNumberDesc(branchCode)
                .map(last -> {
                    long seq = Long.parseLong(last.getAccountNumber().substring(branchCode.length())) + 1;
                    return branchCode + String.format("%07d", seq);
                })
                .orElse(branchCode + "0000001");
    }

}
