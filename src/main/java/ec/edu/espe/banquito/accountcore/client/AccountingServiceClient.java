package ec.edu.espe.banquito.accountcore.client;

import ec.edu.espe.banquito.accountcore.dto.AccountingEntryReqDTO;
import ec.edu.espe.banquito.accountcore.grpc.accounting.AccountingEntryRequest;
import ec.edu.espe.banquito.accountcore.grpc.accounting.AccountingServiceGrpc;
import ec.edu.espe.banquito.accountcore.grpc.accounting.JournalLine;
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

    public void registerEntry(AccountingEntryReqDTO request) {
        accountingService
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                .registerEntry(toGrpcRequest(request));
    }

    @PreDestroy
    public void shutdown() {
        channel.shutdown();
    }

    private AccountingEntryRequest toGrpcRequest(AccountingEntryReqDTO request) {
        AccountingEntryRequest.Builder builder = AccountingEntryRequest.newBuilder()
                .setEntryUuid(request.entryUuid())
                .setDescription(request.description())
                .setEntryDate(request.entryDate().toString());

        request.lines().stream()
                .map(this::toGrpcLine)
                .forEach(builder::addLines);

        return builder.build();
    }

    private JournalLine toGrpcLine(AccountingEntryReqDTO.JournalLineDTO line) {
        return JournalLine.newBuilder()
                .setAccountCode(line.accountCode())
                .setMovementType(line.movementType())
                .setAmount(line.amount().toPlainString())
                .setReference(line.reference())
                .build();
    }
}
