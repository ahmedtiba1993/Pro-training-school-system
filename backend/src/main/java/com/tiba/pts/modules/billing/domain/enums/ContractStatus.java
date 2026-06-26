package com.tiba.pts.modules.billing.domain.enums;

public enum ContractStatus {

  // --- Phase 1: Initialization ---
  DRAFT, // 1. Contract created, pending validation

  // --- Phase 2: Active Lifecycle ---
  ACTIVE, // 2. Contract validated and payments expected

  // --- Phase 3: Terminal States ---
  FULLY_PAID, // 3. Total amount collected, no outstanding balance
  CANCELLED, // 4. Contract voided (enrollment cancelled, refund issued, etc.)
  SETTLED_WITH_DEBT // 5. Payment obligations not met (overdue beyond tolerance)
}
