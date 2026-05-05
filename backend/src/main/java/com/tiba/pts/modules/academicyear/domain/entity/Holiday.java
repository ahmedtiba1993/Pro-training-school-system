package com.tiba.pts.modules.academicyear.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import com.tiba.pts.modules.academicyear.domain.enums.HolidayType;
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
  private String label;

  @Column(nullable = false)
  private LocalDate startDate;

  @Column private LocalDate endDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private HolidayType type;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "academic_year_id", nullable = false)
  private AcademicYear academicYear;

  @Transient
  public long getNumberOfDays() {
    if (startDate == null || endDate == null) {
      return 0;
    }
    return ChronoUnit.DAYS.between(startDate, endDate) + 1;
  }
}
