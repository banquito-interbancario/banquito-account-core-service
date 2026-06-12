package ec.edu.espe.banquito.accountcore.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

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
    @UpdateTimestamp
    private LocalDateTime lastUpdate;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    public CoreParameter() {
    }

    public CoreParameter(String code) {
        this.code = code;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || Hibernate.getClass(this) != Hibernate.getClass(object)) {
            return false;
        }
        CoreParameter parameter = (CoreParameter) object;
        return code != null && Objects.equals(code, parameter.code);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }

    @Override
    public String toString() {
        return "CoreParameter{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", valueString='" + valueString + '\'' +
                ", dataType='" + dataType + '\'' +
                ", lastUpdate=" + lastUpdate +
                '}';
    }
}
