package com.tiba.pts.modules.enrollment.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import com.tiba.pts.modules.enrollment.domain.enums.EnrollmentStatus;
import com.tiba.pts.modules.enrollment.domain.enums.EnrollmentType;
import com.tiba.pts.modules.profiles.domain.entity.Student;
import com.tiba.pts.modules.trainingsession.domain.entity.Promotion;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "enrollments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enrollment extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "enrollment_seq")
  @SequenceGenerator(name = "enrollment_seq", sequenceName = "enrollment_seq")
  private Long id;

  @Column(name = "enrollment_number", unique = true, nullable = false)
  private String enrollmentNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EnrollmentStatus status;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EnrollmentType type;

  @Column(length = 500)
  private String observation;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "promotion_id", nullable = false)
  private Promotion promotion;

  @OneToMany(mappedBy = "enrollment", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<EnrollmentDocumentSubmission> enrollmentDocumentSubmissions = new ArrayList<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_id", nullable = false)
  private Student student;
}
