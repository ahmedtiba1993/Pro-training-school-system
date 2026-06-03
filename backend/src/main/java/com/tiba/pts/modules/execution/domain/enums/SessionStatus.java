package com.tiba.pts.modules.execution.domain.enums;

public enum SessionStatus {
    PLANNED,       // The course is in the future
    IN_PROGRESS,   // The teacher clicked on "Start"
    COMPLETED,     // Course finished, attendance taken, textbook filled
    CANCELED       // Course canceled (teacher absent, etc.)
}
