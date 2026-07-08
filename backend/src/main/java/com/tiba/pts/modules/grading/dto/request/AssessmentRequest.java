package com.tiba.pts.modules.grading.dto.request;

import com.tiba.pts.modules.grading.domain.enums.AssessmentStatus;
import com.tiba.pts.modules.grading.domain.enums.AssessmentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AssessmentRequest(
    @NotNull(message = "PROMOTION_SUBJECT_ID_REQUIRED")
    Long promotionSubjectId,

    @NotBlank(message = "TITLE_REQUIRED")
    String title,

    @NotNull(message = "ASSESSMENT_TYPE_REQUIRED")
    AssessmentType assessmentType,

    @NotNull(message = "TOTAL_MARKS_REQUIRED")
    @DecimalMin(value = "0.0", message = "TOTAL_MARKS_MUST_BE_POSITIVE")
    Double totalMarks,

    @NotNull(message = "WEIGHT_PERCENTAGE_REQUIRED")
    @Min(value = 0, message = "WEIGHT_PERCENTAGE_MUST_BE_POSITIVE")
    @Max(value = 100, message = "WEIGHT_PERCENTAGE_MUST_BE_LESS_THAN_OR_EQUAL_100")
    Integer weightPercentage,

    @NotNull(message = "STATUS_REQUIRED")
    AssessmentStatus status
) {}
