package ec.edu.espe.banquito.accountcore.model;

import ec.edu.espe.banquito.accountcore.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

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
    @NaturalId
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
    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    @Column(name = "is_favorite", nullable = false)
    private Boolean favorite;

    @Column(name = "opening_date", nullable = false)
    @CreationTimestamp
    private LocalDate openingDate;

    @Column(name = "last_update", nullable = false)
    @UpdateTimestamp
    private LocalDateTime lastUpdate;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    public Account() {
    }

    public Account(Long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || Hibernate.getClass(this) != Hibernate.getClass(object)) {
            return false;
        }
        Account account = (Account) object;
        return id != null && Objects.equals(id, account.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }

    @Override
    public String toString() {
        return "Account{" +
                "id=" + id +
                ", customerId=" + customerId +
                ", accountNumber='" + accountNumber + '\'' +
                ", branchId=" + branchId +
                ", availableBalance=" + availableBalance +
                ", accountingBalance=" + accountingBalance +
                ", status=" + status +
                ", favorite=" + favorite +
                ", openingDate=" + openingDate +
                '}';
    }
}
