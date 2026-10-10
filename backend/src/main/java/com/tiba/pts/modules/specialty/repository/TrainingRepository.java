package com.tiba.pts.modules.specialty.repository;

import com.tiba.pts.modules.specialty.domain.entity.Training;
import com.tiba.pts.modules.specialty.domain.enums.TrainingStatus;
import com.tiba.pts.modules.specialty.domain.enums.TrainingType;
import com.tiba.pts.modules.specialty.repository.projection.TrainingTypeCountProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrainingRepository extends JpaRepository<Training, Long> {

  @Override
  @EntityGraph(attributePaths = {"level", "specialty"})
  Optional<Training> findById(Long id);

  @Override
  @EntityGraph(attributePaths = {"level", "specialty"})
  Page<Training> findAll(Pageable pageable);

  boolean existsByLevelIdAndSpecialtyId(Long levelId, Long specialtyId);

  boolean existsByLevelIdAndSpecialtyIdAndTrainingType(
      Long levelId, Long specialtyId, TrainingType trainingType);

  boolean existsByLevelIdAndSpecialtyIdAndTrainingTypeAndIdNot(
      Long levelId, Long specialtyId, TrainingType trainingType, Long id);

  @EntityGraph(attributePaths = {"level", "specialty"})
  List<Training> findAllByStatus(TrainingStatus status);

  @EntityGraph(attributePaths = {"level", "specialty"})
  List<Training> findAllByStatusAndTrainingType(TrainingStatus status, TrainingType type);

  @EntityGraph(attributePaths = {"level", "specialty"})
  List<Training> findAllByStatusAndLevelId(TrainingStatus status, Long levelId);

  @EntityGraph(attributePaths = {"level", "specialty"})
  List<Training> findAllByStatusAndLevelIdAndTrainingType(
      TrainingStatus status, Long levelId, TrainingType type);

  @EntityGraph(attributePaths = {"level", "specialty"})
  List<Training> findAllByLevelId(Long levelId);

  @EntityGraph(attributePaths = {"level", "specialty"})
  List<Training> findAllByLevelIdAndStatusIn(Long levelId, List<TrainingStatus> statuses);

  @Query(
      "SELECT t.trainingType AS trainingType, COUNT(t) AS count "
          + "FROM Training t "
          + "WHERE t.status = :status "
          + "GROUP BY t.trainingType")
  List<TrainingTypeCountProjection> countTrainingsByTypeAndStatus(
      @Param("status") TrainingStatus status);
}
