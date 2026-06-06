package ec.edu.espe.banquito.accountcore.model;

import ec.edu.espe.banquito.accountcore.enums.CatalogStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "INSTITUTIONAL_ACCOUNT")
@Getter
@Setter
public class InstitutionalAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    private String accountNumber;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "accounting_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal accountingBalance;

    @Column(name = "status", nullable = false, length = 15)
    private CatalogStatus status;

    @Column(name = "creation_date", nullable = false)
    private LocalDateTime creationDate;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @PrePersist
    protected void onCreate() {
        this.creationDate = LocalDateTime.now();
    }
}
