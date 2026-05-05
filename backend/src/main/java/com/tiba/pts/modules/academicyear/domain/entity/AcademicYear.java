package com.tiba.pts.modules.academicyear.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import com.tiba.pts.modules.academicyear.domain.enums.YearStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class AcademicYear extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "academic_year_seq")
  @SequenceGenerator(
      name = "academic_year_seq",
      sequenceName = "academic_year_seq",
      allocationSize = 1)
  private Long id;

  @Column(nullable = false)
  private String label;

  @Column(nullable = false)
  private LocalDate startDate;

  @Column(nullable = false)
  private LocalDate endDate;

  @Column(nullable = false)
  private Boolean isDefault;

  @Column(nullable = false)
  private Boolean isLocked;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private YearStatus status;

  @OneToMany(mappedBy = "academicYear", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Period> periods = new ArrayList<>();

  @OneToMany(mappedBy = "academicYear", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Holiday> holidays = new ArrayList<>();
}
