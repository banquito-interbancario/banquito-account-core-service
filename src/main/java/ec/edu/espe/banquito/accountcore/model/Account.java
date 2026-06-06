package ec.edu.espe.banquito.accountcore.model;

import ec.edu.espe.banquito.accountcore.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ACCOUNT")
@Getter
@Setter
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "account_number", unique = true, nullable = false, length = 20)
    private String accountNumber;

    @Column(name = "branch_id", nullable = false)
    private Integer branchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_subtype_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private AccountSubtype accountSubtype;

    @Column(name = "available_balance", precision = 15, scale = 2, nullable = false)
    private BigDecimal availableBalance;

    @Column(name = "accounting_balance", precision = 15, scale = 2, nullable = false)
    private BigDecimal accountingBalance;

    @Column(name = "status", nullable = false, length = 15)
    private AccountStatus status;

    @Column(name = "is_favorite", nullable = false)
    private Boolean favorite;

    @Column(name = "opening_date", nullable = false)
    private LocalDate openingDate;

    @Column(name = "last_update", nullable = false)
    private LocalDateTime lastUpdate;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @PrePersist
    protected void onCreate() {
        if (this.openingDate == null) {
            this.openingDate = LocalDate.now();
        }
        this.lastUpdate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastUpdate = LocalDateTime.now();
    }
}
