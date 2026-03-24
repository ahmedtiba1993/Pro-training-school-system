package com.tiba.pts.modules.enrollment.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import com.tiba.pts.modules.registrationdocuments.domain.entity.RegistrationDocument;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentDocument extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "enrollment_id", nullable = false)
  private Enrollment enrollment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "registration_document_id", nullable = false)
  private RegistrationDocument registrationDocument;

  @Column(name = "is_provided", nullable = false)
  private boolean isProvided = false;
}
