package com.sgs.bank;

import com.sgs.user.AccountState;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class BankDtos {
    private BankDtos() {}

    /** Only amount (+ optional note) is accepted. Date and time are always set by the server. */
    public record AddBankEntryRequest(
            @NotNull @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
            @Digits(integer = 10, fraction = 2, message = "Amount can have at most 2 decimal places") BigDecimal amount,
            @Size(max = 255) String note) {}

    public record EditBankEntryRequest(
            @NotNull @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
            @Digits(integer = 10, fraction = 2, message = "Amount can have at most 2 decimal places") BigDecimal amount,
            @Size(max = 255) String note,
            @NotBlank @Size(max = 255) String reason) {}

    public record BankEntryDto(Long id, BigDecimal amount, String note, LocalDate entryDate, Instant createdAt) {}

    public record DayTotalDto(LocalDate date, BigDecimal total, long entries) {}

    public record BankSummaryDto(LocalDate from, LocalDate to, BigDecimal grandTotal, BigDecimal rangeTotal,
                                 long rangeEntries, List<DayTotalDto> days) {}

    public record AdminBankEntryDto(Long id, Long employeeId, String employeeUsername, String employeeFullName,
                                    BigDecimal amount, String note, LocalDate entryDate, Instant createdAt,
                                    Instant lastModifiedAt, boolean deleted, Instant deletedAt, String deleteReason) {}

    public record EmployeeTotalDto(Long employeeId, String username, String fullName, BigDecimal total, long entries) {}

    public record AdminBankOverviewDto(LocalDate from, LocalDate to, BigDecimal grandTotal,
                                       List<EmployeeTotalDto> employees, List<DayTotalDto> days) {}

    public record AdminEmployeeBankSummaryDto(Long employeeId, String username, String fullName, AccountState state,
                                              BankSummaryDto summary) {}

    public record BankAuditDto(Long id, String action, BigDecimal oldAmount, BigDecimal newAmount, String oldNote,
                               String newNote, String reason, String actedBy, Instant actedAt) {}
}
