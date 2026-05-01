package com.tiba.pts.modules.enrollment.repository;

import com.tiba.pts.modules.enrollment.domain.entity.EnrollmentDocumentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnrollmentDocumentSubmissionRepository
    extends JpaRepository<EnrollmentDocumentSubmission, Long> {}
