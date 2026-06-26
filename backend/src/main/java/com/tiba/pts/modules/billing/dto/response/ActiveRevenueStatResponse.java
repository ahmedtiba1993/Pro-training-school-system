package com.tiba.pts.modules.billing.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActiveRevenueStatResponse {

  /**
   * Active Revenue (In progress)
   */
  private BigDecimal activeRevenue;

  /**
   * Collected (On in-progress training programs)
   */
  private BigDecimal totalCollected;

  /**
   * Remaining to Pay / Outstanding Balance (Global Debt)
   */
  private BigDecimal remainingDebt;

  /**
   * Losses (Dropouts)
   */
  private BigDecimal losses;

  /**
   * Unpaid / Pending bank confirmation
   */
  private BigDecimal pendingPayments;
}
