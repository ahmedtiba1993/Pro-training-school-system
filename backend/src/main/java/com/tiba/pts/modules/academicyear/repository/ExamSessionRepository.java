package com.tiba.pts.modules.academicyear.repository;

import com.tiba.pts.modules.academicyear.domain.entity.ExamSession;
import com.tiba.pts.modules.academicyear.domain.enums.SessionStatus;
import com.tiba.pts.modules.academicyear.domain.enums.SessionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamSessionRepository extends JpaRepository<ExamSession, Long> {

  // Retrieve sessions for a specific period, sorted chronologically
  List<ExamSession> findByPeriodIdOrderByStartDateAsc(Long periodId);

  // --- FOR CREATION ---
  // Check label uniqueness within the same period
  boolean existsByLabelIgnoreCaseAndPeriodId(String label, Long periodId);

  // Check uniqueness of the session type (MAIN, RETAKE) per period
  boolean existsBySessionTypeAndPeriodId(SessionType sessionType, Long periodId);

  // Check for the existence of a session with a specific status per period
  boolean existsByStatusAndPeriodId(SessionStatus status, Long periodId);

  // --- FOR MODIFICATION (Excludes the current ID) ---
  boolean existsByLabelIgnoreCaseAndPeriodIdAndIdNot(String label, Long periodId, Long id);

  boolean existsBySessionTypeAndPeriodIdAndIdNot(SessionType sessionType, Long periodId, Long id);

  boolean existsByStatusAndPeriodIdAndIdNot(SessionStatus status, Long periodId, Long id);
}
