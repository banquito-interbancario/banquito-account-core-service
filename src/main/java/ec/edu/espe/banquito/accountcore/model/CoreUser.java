package ec.edu.espe.banquito.accountcore.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.NaturalId;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "CORE_USER")
@Getter
@Setter
public class CoreUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    @NaturalId
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
    @CreationTimestamp
    private LocalDateTime creationDate;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    public CoreUser() {
    }

    public CoreUser(Integer id) {
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
        CoreUser user = (CoreUser) object;
        return id != null && Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }

    @Override
    public String toString() {
        return "CoreUser{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", role='" + role + '\'' +
                ", status='" + status + '\'' +
                ", branchId=" + branchId +
                ", branchCode='" + branchCode + '\'' +
                ", lastLogin=" + lastLogin +
                '}';
    }
}
