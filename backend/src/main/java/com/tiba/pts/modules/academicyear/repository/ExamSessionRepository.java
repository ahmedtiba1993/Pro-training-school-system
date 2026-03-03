package com.tiba.pts.modules.academicyear.repository;

import com.tiba.pts.modules.academicyear.domain.entity.ExamSession;
import com.tiba.pts.modules.academicyear.domain.enums.SessionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamSessionRepository extends JpaRepository<ExamSession, Long> {

  boolean existsByTermIdAndSessionType(Long termId, SessionType sessionType);

  boolean existsByTermIdAndSessionTypeAndIdNot(Long termId, SessionType sessionType, Long id);

  List<ExamSession> findByTermId(Long termId);
}
