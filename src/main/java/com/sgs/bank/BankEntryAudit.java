package com.sgs.bank;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/** Append-only history of every admin edit / delete / restore on a bank entry. */
@Entity
@Table(name = "bank_entry_audit")
@Getter
@Setter
@NoArgsConstructor
public class BankEntryAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entry_id", nullable = false)
    private Long entryId;

    @Column(nullable = false, length = 20)
    private String action;                       // EDIT, DELETE, RESTORE

    @Column(name = "old_amount", precision = 12, scale = 2)
    private BigDecimal oldAmount;

    @Column(name = "new_amount", precision = 12, scale = 2)
    private BigDecimal newAmount;

    @Column(name = "old_note", length = 255)
    private String oldNote;

    @Column(name = "new_note", length = 255)
    private String newNote;

    @Column(length = 255)
    private String reason;

    @Column(name = "acted_by", nullable = false)
    private Long actedBy;

    @Column(name = "acted_by_username", nullable = false, length = 50)
    private String actedByUsername;

    @Column(name = "acted_at", nullable = false)
    private Instant actedAt;
}
