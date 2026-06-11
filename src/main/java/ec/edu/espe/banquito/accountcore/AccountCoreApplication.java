package ec.edu.espe.banquito.accountcore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AccountCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountCoreApplication.class, args);
    }

}
