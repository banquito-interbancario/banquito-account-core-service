package ec.edu.espe.banquito.accountcore.model;

import ec.edu.espe.banquito.accountcore.enums.TransactionStatus;
import ec.edu.espe.banquito.accountcore.enums.TransactionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "ACCOUNT_TRANSACTION")
@Getter
@Setter
public class AccountTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Account account;

    @Column(name = "transaction_uuid", nullable = false, length = 36)
    private String transactionUuid;

    @Column(name = "movement_type", nullable = false, length = 15)
    @Enumerated(EnumType.STRING)
    private TransactionType movementType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_subtype_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private TransactionSubtype transactionSubtype;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "resulting_balance", precision = 15, scale = 2, nullable = false)
    private BigDecimal resultingBalance;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "status", nullable = false, length = 15)
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @Column(name = "transaction_date", nullable = false)
    @CreationTimestamp
    private LocalDateTime transactionDate;

    @Column(name = "accounting_date", nullable = false)
    private LocalDate accountingDate;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    public AccountTransaction() {
    }

    public AccountTransaction(Long id) {
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
        AccountTransaction transaction = (AccountTransaction) object;
        return id != null && Objects.equals(id, transaction.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }

    @Override
    public String toString() {
        return "AccountTransaction{" +
                "id=" + id +
                ", transactionUuid='" + transactionUuid + '\'' +
                ", movementType=" + movementType +
                ", amount=" + amount +
                ", resultingBalance=" + resultingBalance +
                ", status=" + status +
                ", transactionDate=" + transactionDate +
                ", accountingDate=" + accountingDate +
                '}';
    }
}
