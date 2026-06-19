package ec.edu.espe.banquito.accountcore.client;

import com.banquito.payswitch.notification.NotificationRequest;
import com.banquito.payswitch.notification.NotificationResponse;
import com.banquito.payswitch.notification.NotificationServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class NotificationGrpcClient {

    private final ManagedChannel channel;
    private final NotificationServiceGrpc.NotificationServiceBlockingStub blockingStub;

    public NotificationGrpcClient(
            @Value("${notification.grpc.host:localhost}") String host,
            @Value("${notification.grpc.port:9092}") int port) {
        log.info("Initializing NotificationGrpcClient at {}:{}", host, port);
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.blockingStub = NotificationServiceGrpc.newBlockingStub(channel);
    }

    public void sendNotification(String email, String subject, String bodyTemplate, Map<String, String> variables) {
        log.info("Sending notification to {}", email);
        try {
            NotificationRequest request = NotificationRequest.newBuilder()
                    .setEmailTo(email)
                    .setSubject(subject)
                    .setBodyTemplate(bodyTemplate)
                    .putAllVariables(variables)
                    .build();

            NotificationResponse response = blockingStub
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .sendNotification(request);

            log.info("Notification sent successfully: {}", response.getNotificationId());
        } catch (Exception e) {
            log.error("Failed to send notification via gRPC: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null) {
            channel.shutdown();
        }
    }
}
