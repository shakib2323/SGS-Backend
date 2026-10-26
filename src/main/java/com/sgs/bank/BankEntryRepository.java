package com.sgs.bank;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BankEntryRepository extends JpaRepository<BankEntry, Long>, JpaSpecificationExecutor<BankEntry> {

    @Override
    @EntityGraph(attributePaths = {"employee"})
    Page<BankEntry> findAll(Specification<BankEntry> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"employee"})
    Optional<BankEntry> findById(Long id);

    Page<BankEntry> findByEmployeeIdAndDeletedFalseAndEntryDateBetween(Long employeeId, LocalDate from, LocalDate to, Pageable pageable);

    long countByEmployeeIdAndDeletedFalseAndEntryDateBetween(Long employeeId, LocalDate from, LocalDate to);

    boolean existsByEmployeeIdAndAmountAndDeletedFalseAndCreatedAtAfter(Long employeeId, BigDecimal amount, Instant after);

    @Query("select sum(e.amount) from BankEntry e where e.employee.id = :eid and e.deleted = false")
    BigDecimal sumForEmployee(@Param("eid") Long employeeId);

    @Query("select sum(e.amount) from BankEntry e where e.employee.id = :eid and e.deleted = false "
            + "and e.entryDate between :from and :to")
    BigDecimal sumForEmployeeBetween(@Param("eid") Long employeeId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select e.entryDate as entryDay, sum(e.amount) as totalAmount, count(e) as entryCount "
            + "from BankEntry e where e.employee.id = :eid and e.deleted = false and e.entryDate between :from and :to "
            + "group by e.entryDate order by e.entryDate desc")
    List<DayTotalView> dailyTotalsForEmployee(@Param("eid") Long employeeId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select e.entryDate as entryDay, sum(e.amount) as totalAmount, count(e) as entryCount "
            + "from BankEntry e where e.deleted = false and e.entryDate between :from and :to "
            + "group by e.entryDate order by e.entryDate desc")
    List<DayTotalView> dailyTotalsAll(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select u.id as employeeId, u.username as username, u.fullName as fullName, "
            + "sum(e.amount) as totalAmount, count(e) as entryCount "
            + "from BankEntry e join e.employee u where e.deleted = false and e.entryDate between :from and :to "
            + "group by u.id, u.username, u.fullName order by sum(e.amount) desc")
    List<EmployeeTotalView> employeeTotals(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
