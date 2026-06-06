package ec.edu.espe.banquito.accountcore.repository;

import ec.edu.espe.banquito.accountcore.model.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;

public interface HolidayRepository extends JpaRepository<Holiday, LocalDate> {
    boolean existsByHolidayDate(LocalDate date);
}
