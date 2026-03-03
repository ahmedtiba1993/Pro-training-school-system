package com.tiba.pts.modules.academicyear.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "academic_years")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicYear extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "academic_year_seq_gen")
  @SequenceGenerator(
      name = "academic_year_seq_gen",
      sequenceName = "academic_year_seq",
      allocationSize = 1)
  private Long id;

  @Column(nullable = false, unique = true)
  private String label;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Builder.Default
  @Column(name = "is_active", nullable = false)
  private Boolean isActive = false;

  @OneToMany(mappedBy = "academicYear", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Term> terms = new ArrayList<>();
}
