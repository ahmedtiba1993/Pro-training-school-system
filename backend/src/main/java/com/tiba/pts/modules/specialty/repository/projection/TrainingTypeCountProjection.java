package com.tiba.pts.modules.specialty.repository.projection; // ou dans un sous-package .projection

import com.tiba.pts.modules.specialty.domain.enums.TrainingType;

public interface TrainingTypeCountProjection {
  TrainingType getTrainingType();

  Long getCount();
}
