package ec.edu.espe.banquito.accountcore.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "CORE_PARAMETER")
@Getter
@Setter
public class CoreParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    @Column(name = "value", nullable = false, length = 255)
    private String value;

    @Column(name = "description", length = 255)
    private String description;
}
