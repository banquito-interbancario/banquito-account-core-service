package ec.edu.espe.banquito.accountcore.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

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
    @CreationTimestamp
    private LocalDateTime creationDate;

    public Holiday() {
    }

    public Holiday(LocalDate holidayDate) {
        this.holidayDate = holidayDate;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || Hibernate.getClass(this) != Hibernate.getClass(object)) {
            return false;
        }
        Holiday holiday = (Holiday) object;
        return holidayDate != null && Objects.equals(holidayDate, holiday.holidayDate);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }

    @Override
    public String toString() {
        return "Holiday{" +
                "holidayDate=" + holidayDate +
                ", name='" + name + '\'' +
                ", isWeekend=" + isWeekend +
                '}';
    }
}
