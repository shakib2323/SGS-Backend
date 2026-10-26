package com.sgs.bank;

import java.math.BigDecimal;

public interface EmployeeTotalView {
    Long getEmployeeId();
    String getUsername();
    String getFullName();
    BigDecimal getTotalAmount();
    long getEntryCount();
}
