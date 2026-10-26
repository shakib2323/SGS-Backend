package com.sgs.bank;

import com.sgs.bank.BankDtos.AddBankEntryRequest;
import com.sgs.bank.BankDtos.BankEntryDto;
import com.sgs.bank.BankDtos.BankSummaryDto;
import com.sgs.common.PageDto;
import com.sgs.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** The only bank operations an employee has: add, list own, own totals. No edit, no delete. */
@RestController
@RequestMapping("/api/employee/bank")
@PreAuthorize("hasRole('EMPLOYEE')")
@RequiredArgsConstructor
public class EmployeeBankController {

    private final BankService service;

    @PostMapping
    public ResponseEntity<BankEntryDto> add(@Valid @RequestBody AddBankEntryRequest r, @AuthenticationPrincipal AuthUser actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.add(actor, r));
    }

    @GetMapping
    public PageDto<BankEntryDto> list(@RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                      @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                      @RequestParam(name = "page", defaultValue = "0") int page,
                                      @RequestParam(name = "size", defaultValue = "30") int size,
                                      @AuthenticationPrincipal AuthUser actor) {
        return service.listOwn(actor, from, to, page, size);
    }

    /** grandTotal = all-time total; rangeTotal + days[] = date-wise totals for the selected range. */
    @GetMapping("/summary")
    public BankSummaryDto summary(@RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                  @AuthenticationPrincipal AuthUser actor) {
        return service.summaryOwn(actor, from, to);
    }
}
