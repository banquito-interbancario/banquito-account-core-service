package ec.edu.espe.banquito.accountcore.repository;

import ec.edu.espe.banquito.accountcore.model.CoreParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CoreParameterRepository extends JpaRepository<CoreParameter, String> {
    Optional<CoreParameter> findByCode(String code);
}
