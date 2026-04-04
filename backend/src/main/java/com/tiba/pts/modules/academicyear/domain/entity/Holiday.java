package com.tiba.pts.modules.academicyear.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Holiday extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "holiday_seq")
  @SequenceGenerator(name = "holiday_seq", sequenceName = "holiday_seq", allocationSize = 1)
  private Long id;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private LocalDate startDate;

  @Column private LocalDate endDate;

  @Column(nullable = false)
  private Long numberOfDays;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "academic_year_id", nullable = false)
  private AcademicYear academicYear;

@PrePersist
  @PreUpdate
  public void calculateNumberOfDays() {
    if (startDate != null) {
      if (endDate != null) {
        // Case 1: Vacation period (e.g., 3 days)
        this.numberOfDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
      } else {
        // Case 2: Single public holiday (endDate is null)
        this.numberOfDays = 1L;
      }
    }
  }
}
