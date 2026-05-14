package com.tiba.pts.modules.enrollment.domain.enums;

public enum EnrollmentStatus {

  // --- Phase 1: Input & Processing ---
  PRE_ENROLLED, // 1. Draft, pending processing
  INCOMPLETE, // 2. Processed, but blocking documents missing
  WAITLISTED, // 3. Good file, but class is full (Waiting list)

  // --- Phase 2: Active Enrollment ---
  CONDITIONALLY_VALIDATED, // 4. Accepted into class, account created, but minor documents missing
  VALIDATED, // 5. Official and perfect enrollment

  // --- Phase 3: Course Incidents ---
  SUSPENDED, // 6. Temporary pause (Illness, non-payment, disciplinary action)
  DROPPED_OUT, // 7. Withdrawn during the school year (Permanent)

  // --- Phase 4: End-of-Life (Terminal States) ---
  REJECTED, // 8. Rejected by the school at admission
  CANCELLED, // 9. Canceled by the student BEFORE the start of classes
  COMPLETED // 10. Year completed successfully (Natural end)
}
