package ec.edu.espe.banquito.accountcore.model;

import ec.edu.espe.banquito.accountcore.enums.TransactionStatus;
import ec.edu.espe.banquito.accountcore.enums.TransactionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private TransactionStatus status;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "accounting_date", nullable = false)
    private LocalDate accountingDate;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;
}
