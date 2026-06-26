package com.tiba.pts.modules.billing.repository.projection;

import java.math.BigDecimal;

/**
 * Spring Data projection interface for aggregated financial statistics.
 * Used by unified repository queries to return multiple SUM results in a single DB round-trip.
 */
public interface FinancialStatProjection {

  /** Active Revenue */
  BigDecimal getActiveRevenue();

  /** Total Collected */
  BigDecimal getTotalCollected();

  /** Remaining Debt */
  BigDecimal getRemainingDebt();

  /** Losses */
  BigDecimal getLosses();
}
