package com.sgs.bank;

import com.sgs.common.BaseEntity;
import com.sgs.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Money an employee received from the company. createdAt is the server-generated entry time.
 * Employees can only INSERT (no update/delete endpoint exists for them); admin changes are soft-deletes/edits
 * that are always recorded in bank_entry_audit.
 */
@Entity
@Table(name = "bank_entries")
@Getter
@Setter
@NoArgsConstructor
public class BankEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false, updatable = false)
    private User employee;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String note;

    /** Calendar date of the entry in the business timezone (Asia/Kolkata), for date-wise totals. */
    @Column(name = "entry_date", nullable = false, updatable = false)
    private LocalDate entryDate;

    @Column(nullable = false)
    private boolean deleted;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by")
    private Long deletedBy;

    @Column(name = "delete_reason", length = 255)
    private String deleteReason;
}
