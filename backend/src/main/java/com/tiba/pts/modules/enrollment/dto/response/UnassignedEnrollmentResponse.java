package com.tiba.pts.modules.enrollment.dto.response;

import java.time.LocalDate;

public record UnassignedEnrollmentResponse(
    Long enrollmentId,
    String enrollmentNumber,
    String firstName,
    String lastName,
    String studentCode,
    LocalDate birthDate) {}
