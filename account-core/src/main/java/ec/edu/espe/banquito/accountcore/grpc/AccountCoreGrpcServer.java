package ec.edu.espe.banquito.accountcore.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AccountCoreGrpcServer {

    private final int port;
    private final AccountCoreGrpcService accountCoreGrpcService;
    private Server server;

    public AccountCoreGrpcServer(
            @Value("${account-core.grpc.port:9091}") int port,
            AccountCoreGrpcService accountCoreGrpcService) {
        this.port = port;
        this.accountCoreGrpcService = accountCoreGrpcService;
    }

    @PostConstruct
    public void start() throws IOException {
        server = ServerBuilder
                .forPort(port)
                .addService(accountCoreGrpcService)
                .build()
                .start();
    }

    @PreDestroy
    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }
}
