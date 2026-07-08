package com.tiba.pts.modules.grading.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AssessmentGradesRequest(
    @NotNull(message = "ASSESSMENT_ID_REQUIRED")
    Long assessmentId,

    @NotEmpty(message = "GRADES_LIST_REQUIRED")
    @Valid
    List<GradeRecordInput> grades
) {}
