package ec.edu.espe.banquito.accountcore.repository;

import ec.edu.espe.banquito.accountcore.model.CoreUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CoreUserRepository extends JpaRepository<CoreUser, Integer> {
    Optional<CoreUser> findByUsername(String username);
}
