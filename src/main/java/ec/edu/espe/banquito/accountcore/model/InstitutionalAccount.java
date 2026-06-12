package ec.edu.espe.banquito.accountcore.model;

import ec.edu.espe.banquito.accountcore.enums.CatalogStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.NaturalId;

@Entity
@Table(name = "INSTITUTIONAL_ACCOUNT")
@Getter
@Setter
public class InstitutionalAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    @NaturalId
    private String accountNumber;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "accounting_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal accountingBalance;

    @Column(name = "status", nullable = false, length = 15)
    @Enumerated(EnumType.STRING)
    private CatalogStatus status;

    @Column(name = "creation_date", nullable = false)
    @CreationTimestamp
    private LocalDateTime creationDate;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    public InstitutionalAccount() {
    }

    public InstitutionalAccount(Integer id) {
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
        InstitutionalAccount account = (InstitutionalAccount) object;
        return id != null && Objects.equals(id, account.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }

    @Override
    public String toString() {
        return "InstitutionalAccount{" +
                "id=" + id +
                ", accountNumber='" + accountNumber + '\'' +
                ", name='" + name + '\'' +
                ", accountingBalance=" + accountingBalance +
                ", status=" + status +
                '}';
    }
}
