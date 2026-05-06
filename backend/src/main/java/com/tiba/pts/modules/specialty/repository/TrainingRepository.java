package com.tiba.pts.modules.specialty.repository;

import com.tiba.pts.modules.specialty.domain.entity.Training;
import com.tiba.pts.modules.specialty.domain.enums.TrainingStatus;
import com.tiba.pts.modules.specialty.domain.enums.TrainingType;
import com.tiba.pts.modules.specialty.repository.projection.TrainingTypeCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TrainingRepository extends JpaRepository<Training, Long> {

  // Method to check for uniqueness
  boolean existsByLevelIdAndSpecialtyId(Long levelId, Long specialtyId);

  boolean existsByLevelIdAndSpecialtyIdAndTrainingType(
      Long levelId, Long specialtyId, TrainingType trainingType);

  // Retrieve all trainings by status
  List<Training> findAllByStatus(TrainingStatus status);

  // Retrieve trainings by status AND type
  List<Training> findAllByStatusAndTrainingType(TrainingStatus status, TrainingType type);

  List<Training> findAllByStatusAndLevelId(TrainingStatus status, Long levelId);

  // Add this method in your interface
  boolean existsByLevelIdAndSpecialtyIdAndTrainingTypeAndIdNot(
      Long levelId, Long specialtyId, TrainingType trainingType, Long id);

  @Query(
      "SELECT t.trainingType AS trainingType, COUNT(t) AS count "
          + "FROM Training t "
          + "WHERE t.status = :status "
          + "GROUP BY t.trainingType")
  List<TrainingTypeCountProjection> countTrainingsByTypeAndStatus(
      @Param("status") TrainingStatus status);
}
