package com.tiba.pts.modules.schedule.repository;

import com.tiba.pts.modules.schedule.domain.entity.TimeSlotDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface TimeSlotDefinitionRepository extends JpaRepository<TimeSlotDefinition, Long> {

  // List sorted by orderIndex ascending
  List<TimeSlotDefinition> findAllByOrderByOrderIndexAsc();

  boolean existsByCode(String code);

  boolean existsByCodeAndIdNot(String code, Long id);

  // Anti-overlap check (Creation)
  @Query(
      "SELECT COUNT(t) > 0 FROM TimeSlotDefinition t WHERE t.startTime < :endTime AND t.endTime > :startTime")
  boolean existsByOverlap(
      @Param("startTime") LocalTime startTime, @Param("endTime") LocalTime endTime);

  // Anti-overlap check excluding the current time slot (Modification)
  @Query(
      "SELECT COUNT(t) > 0 FROM TimeSlotDefinition t WHERE t.id != :id AND t.startTime < :endTime AND t.endTime > :startTime")
  boolean existsByOverlapExcludingId(
      @Param("id") Long id,
      @Param("startTime") LocalTime startTime,
      @Param("endTime") LocalTime endTime);
}
