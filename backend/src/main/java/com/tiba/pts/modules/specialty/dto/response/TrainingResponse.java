package com.tiba.pts.modules.specialty.dto.response;

import com.tiba.pts.modules.specialty.domain.enums.DurationUnit;
import com.tiba.pts.modules.specialty.domain.enums.TrainingStatus;
import com.tiba.pts.modules.specialty.domain.enums.TrainingType;
import java.time.LocalDateTime;

public record TrainingResponse(
    Long id,
    String code,
    String description,
    TrainingType trainingType,
    Integer durationValue,
    DurationUnit durationUnit,
    TrainingStatus status,

    // --- Fields for Level ---
    Long levelId,
    String levelCode,
    String levelLabel,

    // --- Fields for Specialty ---
    Long specialtyId,
    String specialtyCode,
    String specialtyLabel) {}
