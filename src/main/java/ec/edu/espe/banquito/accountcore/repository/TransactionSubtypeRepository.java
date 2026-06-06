package ec.edu.espe.banquito.accountcore.repository;

import ec.edu.espe.banquito.accountcore.model.TransactionSubtype;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionSubtypeRepository extends JpaRepository<TransactionSubtype, Integer> {
    Optional<TransactionSubtype> findByCode(String code);
}
