package com.tiba.pts.modules.trainingsession.domain.entity;

import com.tiba.pts.modules.academicyear.domain.entity.AcademicYear;
import jakarta.persistence.*;
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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "academic_year_id", nullable = false)
  private AcademicYear academicYear;

  @PrePersist
  @PreUpdate
  public void syncDatesWithAcademicYear() {
    if (this.academicYear != null) {
      this.setStartDate(this.academicYear.getStartDate());
      this.setEndDate(this.academicYear.getEndDate());
    }
  }
}
