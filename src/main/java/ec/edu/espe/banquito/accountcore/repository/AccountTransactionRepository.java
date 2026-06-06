package ec.edu.espe.banquito.accountcore.repository;

import ec.edu.espe.banquito.accountcore.model.AccountTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, Long> {
    boolean existsByTransactionUuidAndTransactionDateAfter(String uuid, LocalDateTime date);

    @Query("""
            select transaction
            from AccountTransaction transaction
            where transaction.account.id = :accountId
              and (:fromDate is null or transaction.accountingDate >= :fromDate)
              and (:toDate is null or transaction.accountingDate <= :toDate)
            order by transaction.transactionDate desc
            """)
    Page<AccountTransaction> findHistory(
            @Param("accountId") Long accountId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable);
}
