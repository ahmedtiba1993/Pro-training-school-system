package com.tiba.pts.modules.enrollment.repository.projection;

public interface EnrollmentGenderStatsProjection {
  long getTotalCount();
  long getMaleCount();
  long getFemaleCount();
}
