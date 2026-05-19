package com.expensetracker.repository.projection;

import java.math.BigDecimal;

public interface CategorySpendSummary {

    Long getCategoryId();

    String getCategoryName();

    String getCategoryIcon();

    BigDecimal getTotalAmount();
}
