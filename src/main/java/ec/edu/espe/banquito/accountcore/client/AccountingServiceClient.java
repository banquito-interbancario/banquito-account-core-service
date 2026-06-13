package ec.edu.espe.banquito.accountcore.client;

import ec.edu.espe.banquito.accountcore.dto.AccountingOperationReqDTO;
import ec.edu.espe.banquito.accountcore.grpc.accounting.AccountingOperationRequest;
import ec.edu.espe.banquito.accountcore.grpc.accounting.AccountingServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class AccountingServiceClient {

    private final ManagedChannel channel;
    private final AccountingServiceGrpc.AccountingServiceBlockingStub accountingService;

    public AccountingServiceClient(
            @Value("${accounting.grpc.host:localhost}") String accountingGrpcHost,
            @Value("${accounting.grpc.port:9092}") int accountingGrpcPort) {
        this.channel = ManagedChannelBuilder
                .forAddress(accountingGrpcHost, accountingGrpcPort)
                .usePlaintext()
                .build();
        this.accountingService = AccountingServiceGrpc.newBlockingStub(channel);
    }

    public void postOperation(AccountingOperationReqDTO request) {
        accountingService
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                .postOperation(toGrpcRequest(request));
    }

    @PreDestroy
    public void shutdown() {
        channel.shutdown();
    }

    private AccountingOperationRequest toGrpcRequest(AccountingOperationReqDTO request) {
        AccountingOperationRequest.Builder builder = AccountingOperationRequest.newBuilder()
                .setOperationUuid(request.operationUuid())
                .setOperationType(request.operationType().name())
                .setAccountProductType(request.accountProductType().name())
                .setAmount(request.amount().toPlainString())
                .setReference(request.reference() == null ? "" : request.reference())
                .setAccountingDate(request.accountingDate().toString());
        if (request.commissionAmount() != null) {
            builder.setCommissionAmount(request.commissionAmount().toPlainString());
        }
        return builder
                .build();
    }
}
