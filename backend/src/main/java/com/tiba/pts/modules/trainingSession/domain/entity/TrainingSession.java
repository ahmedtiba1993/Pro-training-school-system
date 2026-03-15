package com.tiba.pts.modules.trainingSession.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import com.tiba.pts.modules.academicyear.domain.entity.AcademicYear;
import com.tiba.pts.modules.specialty.domain.entity.Level;
import com.tiba.pts.modules.specialty.domain.entity.Specialty;
import com.tiba.pts.modules.trainingSession.domain.enums.TrainingSessionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Table(
    name = "training_sessions",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_training_session_academic_level_specialty",
          columnNames = {"academic_year_id", "level_id", "specialty_id"})
    })
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TrainingSession extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "training_session_seq")
  @SequenceGenerator(
      name = "training_session_seq",
      sequenceName = "training_session_seq",
      allocationSize = 1)
  private Long id;

  @Column(name = "promotion_name", unique = true, nullable = false)
  private String promotionName;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "academic_year_id", nullable = false)
  private AcademicYear academicYear;

  @Column(name = "enrolled_count", nullable = false)
  private Integer enrolledCount = 0;

  @Column(name = "expected_start_date", nullable = false)
  private LocalDate expectedStartDate;

  @Column(name = "estimated_end_date", nullable = false)
  private LocalDate estimatedEndDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private TrainingSessionStatus status;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "level_id", nullable = false)
  private Level level;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "specialty_id", nullable = false)
  private Specialty specialty;

  @Column(name = "registration_open_date", nullable = false)
  private LocalDate registrationOpenDate;

  @Column(name = "registration_deadline", nullable = false)
  private LocalDate registrationDeadline;

  @Column(name = "registrations_open", nullable = false)
  private Boolean registrationsOpen;
}
