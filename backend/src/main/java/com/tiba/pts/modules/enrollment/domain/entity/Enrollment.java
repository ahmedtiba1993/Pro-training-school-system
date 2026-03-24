package com.tiba.pts.modules.enrollment.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import com.tiba.pts.modules.person.domain.entity.Student;
import com.tiba.pts.modules.enrollment.domain.enums.EnrollmentStatus;
import com.tiba.pts.modules.trainingSession.domain.entity.TrainingSession;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "enrollments")
public class Enrollment extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "training_session_id", nullable = false)
  private TrainingSession trainingSession;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EnrollmentStatus status;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "student_id", nullable = false)
  private Student student;

  @OneToMany(mappedBy = "enrollment", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<EnrollmentDocument> providedDocuments = new ArrayList<>();
}
