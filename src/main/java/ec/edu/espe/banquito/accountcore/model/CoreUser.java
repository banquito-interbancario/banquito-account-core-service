package ec.edu.espe.banquito.accountcore.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "CORE_USER")
@Getter
@Setter
public class CoreUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "role", nullable = false, length = 50)
    private String role;

    @Column(name = "status", nullable = false, length = 15)
    private String status;

    @Column(name = "branch_id")
    private Integer branchId;

    @Column(name = "branch_code", length = 20)
    private String branchCode;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

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
