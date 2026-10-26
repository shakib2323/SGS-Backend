package com.sgs.bank;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BankEntryAuditRepository extends JpaRepository<BankEntryAudit, Long> {
    List<BankEntryAudit> findByEntryIdOrderByIdDesc(Long entryId);
}
