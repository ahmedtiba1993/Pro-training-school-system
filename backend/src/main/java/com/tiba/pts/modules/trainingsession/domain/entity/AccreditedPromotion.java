package com.tiba.pts.modules.trainingsession.domain.entity;

import com.tiba.pts.modules.academicyear.domain.entity.AcademicYear;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccreditedPromotion extends Promotion {

  private LocalDate registrationOpeningDate;
  private LocalDate registrationDeadline;

  @ManyToOne
  @JoinColumn(name = "academic_year_id", nullable = false)
  private AcademicYear academicYear;
}
