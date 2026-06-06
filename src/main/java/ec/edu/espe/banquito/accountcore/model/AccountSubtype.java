package ec.edu.espe.banquito.accountcore.model;

import ec.edu.espe.banquito.accountcore.enums.AccountSuperType;
import ec.edu.espe.banquito.accountcore.enums.CatalogStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "ACCOUNT_SUBTYPE")
@Getter
@Setter
public class AccountSubtype {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "super_type", nullable = false, length = 15)
    private AccountSuperType superType;

    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "status", nullable = false, length = 15)
    private CatalogStatus status;

    @Column(name = "observations", length = 255)
    private String observations;

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
