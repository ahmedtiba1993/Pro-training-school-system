package com.tiba.pts.modules.specialty.repository;

import com.tiba.pts.modules.specialty.domain.entity.Training;
import com.tiba.pts.modules.specialty.domain.enums.TrainingType;
import com.tiba.pts.modules.specialty.repository.projection.TrainingTypeCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TrainingRepository extends JpaRepository<Training, Long> {

  // Method to check for uniqueness
  boolean existsByLevelIdAndSpecialtyId(Long levelId, Long specialtyId);

  List<Training> findByIsActiveTrue();

  // METHOD FOR UPDATE
  boolean existsByLevelIdAndSpecialtyIdAndIdNot(Long levelId, Long specialtyId, Long id);

  @Query(
      "SELECT t.trainingType AS trainingType, COUNT(t) AS count FROM Training t WHERE t.isActive = true GROUP BY t.trainingType")
  List<TrainingTypeCountProjection> countActiveTrainingsByType();
}
