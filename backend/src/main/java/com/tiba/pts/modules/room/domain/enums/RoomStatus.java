package com.tiba.pts.modules.room.domain.enums;

public enum RoomStatus {
  DRAFT, // Under construction (invisible for planning)
  ACTIVE, // Operational
  MAINTENANCE, // Temporarily unavailable (blocks new reservations)
  ARCHIVED // Immutable terminal state
}
