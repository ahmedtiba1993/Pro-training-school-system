package com.tiba.pts.modules.profiles.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class RefTeacherSpecialty extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "specialty_teacher_seq")
  @SequenceGenerator(
      name = "specialty_teacher_seq",
      sequenceName = "specialty_teacher_seq",
      allocationSize = 1)
  private Long id;

  @Column(nullable = false, unique = true)
  private String code;

  @Column(nullable = false, unique = true)
  private String label;

  @Column(length = 500)
  private String description;

  @ManyToMany(mappedBy = "specialties", fetch = FetchType.LAZY)
  @Builder.Default
  private Set<Teacher> teachers = new HashSet<>();
}
