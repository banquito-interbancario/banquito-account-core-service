package ec.edu.espe.banquito.accountcore.repository;

import ec.edu.espe.banquito.accountcore.model.InstitutionalAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstitutionalAccountRepository extends JpaRepository<InstitutionalAccount, Integer> {
}
