package com.tiba.pts.modules.profiles.domain.enums;

public enum StudentStatus {
  PROSPECT, // File created, no validated enrollment
  ACTIVE, // Currently studying
  SUSPENDED, // Temporary exclusion or blocked
  DROPPED_OUT, // Dropout or no re-enrollment
  ALUMNI // Graduated / Course completed
}
