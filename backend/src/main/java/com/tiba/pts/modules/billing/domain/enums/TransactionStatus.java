package com.tiba.pts.modules.billing.domain.enums;

public enum TransactionStatus {

  PENDING,    // Payment recorded but funds not yet guaranteed (e.g., check deposited, promised transfer)
  CLEARED,    // Funds officially received — the only status that reduces the student's debt
  BOUNCED,    // Payment rejected (e.g., insufficient funds, declined card)
  CANCELLED,  // Payment cancelled before funds were guaranteed (e.g., check never deposited, transfer aborted)
  REFUNDED    // Funds returned to the parent (e.g., enrollment cancellation)
}
