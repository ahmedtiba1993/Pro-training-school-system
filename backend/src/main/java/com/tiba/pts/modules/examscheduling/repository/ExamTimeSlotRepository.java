package com.tiba.pts.modules.examscheduling.repository;

import com.tiba.pts.modules.examscheduling.domain.entity.ExamTimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamTimeSlotRepository extends JpaRepository<ExamTimeSlot, Long> {

  boolean existsByCodeIgnoreCase(String code);

  List<ExamTimeSlot> findByIsActiveTrue();
}
