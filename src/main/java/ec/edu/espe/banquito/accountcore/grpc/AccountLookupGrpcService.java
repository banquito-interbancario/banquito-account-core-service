package ec.edu.espe.banquito.accountcore.grpc;

import ec.edu.espe.banquito.accountcore.enums.AccountStatus;
import ec.edu.espe.banquito.accountcore.exception.AccountNotFoundException;
import ec.edu.espe.banquito.accountcore.grpc.accountlookup.AccountLookupResponse;
import ec.edu.espe.banquito.accountcore.grpc.accountlookup.AccountLookupServiceGrpc;
import ec.edu.espe.banquito.accountcore.grpc.accountlookup.GetAccountByNumberRequest;
import ec.edu.espe.banquito.accountcore.model.Account;
import ec.edu.espe.banquito.accountcore.repository.AccountRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;

@Component
public class AccountLookupGrpcService extends AccountLookupServiceGrpc.AccountLookupServiceImplBase {

    private final AccountRepository accountRepository;

    public AccountLookupGrpcService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public void getAccountByNumber(GetAccountByNumberRequest request,
                                   StreamObserver<AccountLookupResponse> responseObserver) {
        if (request.getAccountNumber().isBlank()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Account number is required")
                    .asRuntimeException());
            return;
        }

        try {
            Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
                    .orElseThrow(() -> new AccountNotFoundException(request.getAccountNumber()));

            responseObserver.onNext(AccountLookupResponse.newBuilder()
                    .setAccountId(account.getId())
                    .setAccountNumber(account.getAccountNumber())
                    .setCustomerId(account.getCustomerId())
                    .setStatus(toLegacyStatus(account.getStatus()))
                    .build());
            responseObserver.onCompleted();
        } catch (AccountNotFoundException exception) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(exception.getMessage())
                    .asRuntimeException());
        }
    }

    private String toLegacyStatus(AccountStatus status) {
        return switch (status) {
            case ACTIVE -> "ACTIVA";
            case INACTIVE -> "INACTIVA";
            case BLOCKED -> "BLOQUEADA";
            case SUSPENDED -> "SUSPENDIDA";
        };
    }
}
