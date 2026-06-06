package ec.edu.espe.banquito.accountcore.model;

import ec.edu.espe.banquito.accountcore.enums.CatalogStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "TRANSACTION_SUBTYPE")
@Getter
@Setter
public class TransactionSubtype {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "status", nullable = false, length = 15)
    private CatalogStatus status;

    @Column(name = "creation_date", nullable = false)
    private java.time.LocalDateTime creationDate;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @PrePersist
    protected void onCreate() {
        this.creationDate = java.time.LocalDateTime.now();
    }
}
