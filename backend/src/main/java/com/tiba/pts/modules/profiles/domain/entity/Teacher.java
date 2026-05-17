package com.tiba.pts.modules.profiles.domain.entity;

import com.tiba.pts.modules.profiles.domain.enums.ContractType;
import com.tiba.pts.modules.profiles.domain.enums.AcademicDegree;
import com.tiba.pts.modules.profiles.domain.enums.TeacherStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Teacher extends Person {

  @Column(nullable = false, unique = true)
  private String code;

  @Enumerated(EnumType.STRING)
  @Column(name = "contract_type", nullable = false)
  private ContractType contractType;

  @Column(name = "hire_date", nullable = false)
  private LocalDate hireDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private TeacherStatus status = TeacherStatus.ONBOARDING;

  @Enumerated(EnumType.STRING)
  @Column(name = "academic_degree", nullable = false)
  private AcademicDegree degree;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "teacher_specialties",
      joinColumns = @JoinColumn(name = "teacher_id"),
      inverseJoinColumns = @JoinColumn(name = "specialty_id"))
  @Builder.Default
  private Set<RefTeacherSpecialty> specialties = new HashSet<>();
}
