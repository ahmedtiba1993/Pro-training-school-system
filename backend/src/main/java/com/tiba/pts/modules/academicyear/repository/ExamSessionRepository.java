package com.tiba.pts.modules.academicyear.repository;

import com.tiba.pts.modules.academicyear.domain.entity.ExamSession;
import com.tiba.pts.modules.academicyear.domain.enums.SessionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExamSessionRepository extends JpaRepository<ExamSession, Long> {

  // To quickly check if a session (MAIN or RETAKE) already exists
  boolean existsByPeriodIdAndSessionType(Long periodId, SessionType sessionType);

  // To retrieve the MAIN session (required for comparing RETAKE dates)
  Optional<ExamSession> findByPeriodIdAndSessionType(Long periodId, SessionType sessionType);

  // Retrieve sessions for a specific period, sorted chronologically
  List<ExamSession> findByPeriodIdOrderByStartDateAsc(Long periodId);

  // --- FOR CREATION ---
  // Check label uniqueness within the same period
  boolean existsByLabelIgnoreCaseAndPeriodId(String label, Long periodId);

  // Check uniqueness of the session type (MAIN, RETAKE) per period
  boolean existsBySessionTypeAndPeriodId(SessionType sessionType, Long periodId);

  // --- FOR MODIFICATION (Excludes the current ID) ---
  boolean existsByLabelIgnoreCaseAndPeriodIdAndIdNot(String label, Long periodId, Long id);

  boolean existsBySessionTypeAndPeriodIdAndIdNot(SessionType sessionType, Long periodId, Long id);

  // To find the latest end date for a specific exam type (e.g.: RETAKE)
  @Query(
      "SELECT MAX(e.endDate) FROM ExamSession e WHERE e.period.academicYear.id = :yearId AND e.sessionType = :sessionType")
  Optional<LocalDate> findMaxEndDateByAcademicYearIdAndSessionType(
      @Param("yearId") Long yearId, @Param("sessionType") SessionType sessionType);
}
