package com.tiba.pts.modules.enrollment.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import com.tiba.pts.modules.documents.domain.entity.EnrollmentDocument;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollmentDocumentSubmission extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "enrollment_submitted_doc_seq")
  @SequenceGenerator(
      name = "enrollment_submitted_doc_seq",
      sequenceName = "enrollment_submitted_doc_seq")
  private Long id;

  @Column(name = "is_provided", nullable = false)
  private boolean provided;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "enrollment_id", nullable = false)
  private Enrollment enrollment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "document_id", nullable = false)
  private EnrollmentDocument document;
}
