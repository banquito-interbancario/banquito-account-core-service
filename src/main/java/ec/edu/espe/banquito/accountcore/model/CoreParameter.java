package ec.edu.espe.banquito.accountcore.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "CORE_PARAMETER")
@Getter
@Setter
public class CoreParameter {

    @Id
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "value_string", nullable = false, length = 255)
    private String valueString;

    @Column(name = "data_type", nullable = false, length = 20)
    private String dataType;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "last_update", nullable = false)
    private LocalDateTime lastUpdate;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.lastUpdate = LocalDateTime.now();
    }
}
