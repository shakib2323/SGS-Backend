package com.sgs.bank;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DayTotalView {
    LocalDate getEntryDay();
    BigDecimal getTotalAmount();
    long getEntryCount();
}
