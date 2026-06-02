package ec.edu.espe.banquito.accountcore.client;

import ec.edu.espe.banquito.accountcore.grpc.party.CustomerResponse;
import ec.edu.espe.banquito.accountcore.grpc.party.GetCustomerByAccountRequest;
import ec.edu.espe.banquito.accountcore.grpc.party.GetCustomerRequest;
import ec.edu.espe.banquito.accountcore.grpc.party.PartyServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class PartyServiceClient {

    private final ManagedChannel channel;
    private final PartyServiceGrpc.PartyServiceBlockingStub partyService;

    public PartyServiceClient(
            @Value("${party.grpc.host:localhost}") String partyGrpcHost,
            @Value("${party.grpc.port:9093}") int partyGrpcPort) {
        this.channel = ManagedChannelBuilder
                .forAddress(partyGrpcHost, partyGrpcPort)
                .usePlaintext()
                .build();
        this.partyService = PartyServiceGrpc.newBlockingStub(channel);
    }

    public void validateActiveCustomer(Long customerId) {
        CustomerResponse customer = partyService
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                .getCustomer(GetCustomerRequest.newBuilder().setCustomerId(customerId).build());
        if (!isActive(customer.getStatus())) {
            throw new IllegalStateException("Customer is not active: " + customerId);
        }
    }

    public String getHolderNameByAccount(String accountNumber) {
        return partyService
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                .getCustomerByAccount(GetCustomerByAccountRequest.newBuilder().setAccountNumber(accountNumber).build())
                .getHolderName();
    }

    @PreDestroy
    public void shutdown() {
        channel.shutdown();
    }

    private boolean isActive(String status) {
        return "ACTIVE".equalsIgnoreCase(status) || "ACTIVO".equalsIgnoreCase(status);
    }
}
