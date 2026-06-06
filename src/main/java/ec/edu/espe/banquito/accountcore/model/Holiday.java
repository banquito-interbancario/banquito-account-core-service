package ec.edu.espe.banquito.accountcore.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "HOLIDAY")
@Getter
@Setter
public class Holiday {

    @Id
    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "is_weekend", nullable = false)
    private Boolean isWeekend;

    @Column(name = "creation_date", nullable = false)
    private LocalDateTime creationDate;

    @PrePersist
    protected void onCreate() {
        this.creationDate = LocalDateTime.now();
    }
}
