package ec.edu.espe.banquito.accountcore.model;

import ec.edu.espe.banquito.accountcore.enums.AccountSuperType;
import ec.edu.espe.banquito.accountcore.enums.CatalogStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.NaturalId;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "ACCOUNT_SUBTYPE")
@Getter
@Setter
public class AccountSubtype {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "super_type", nullable = false, length = 15)
    @Enumerated(EnumType.STRING)
    private AccountSuperType superType;

    @Column(name = "code", nullable = false, unique = true, length = 30)
    @NaturalId
    private String code;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "status", nullable = false, length = 15)
    @Enumerated(EnumType.STRING)
    private CatalogStatus status;

    @Column(name = "observations", length = 255)
    private String observations;

    @Column(name = "creation_date", nullable = false)
    @CreationTimestamp
    private LocalDateTime creationDate;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    public AccountSubtype() {
    }

    public AccountSubtype(Integer id) {
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
        AccountSubtype subtype = (AccountSubtype) object;
        return id != null && Objects.equals(id, subtype.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }

    @Override
    public String toString() {
        return "AccountSubtype{" +
                "id=" + id +
                ", superType=" + superType +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", status=" + status +
                '}';
    }
}
