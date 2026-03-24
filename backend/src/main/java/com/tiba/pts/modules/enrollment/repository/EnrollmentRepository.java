package com.tiba.pts.modules.enrollment.repository;

import com.tiba.pts.modules.enrollment.domain.entity.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

  // Load everything at once for the dashboard display!
  @EntityGraph(
      attributePaths = {
        "student",
        "student.parents", // To retrieve the guardian
        "trainingSession",
        "trainingSession.level", // To display the level
        "trainingSession.specialty" // To display the specialty
      })
  Page<Enrollment> findAll(Pageable pageable);
}
