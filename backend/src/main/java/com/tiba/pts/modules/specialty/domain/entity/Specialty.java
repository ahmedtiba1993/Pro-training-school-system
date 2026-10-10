package com.tiba.pts.modules.specialty.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Specialty extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "specialty_seq")
  @SequenceGenerator(name = "specialty_seq", sequenceName = "specialty_seq", allocationSize = 1)
  private Long id;

  @Column(nullable = false)
  private String label;

  @Column(nullable = false, unique = true)
  private String code;

  @Column(nullable = false)
  @Builder.Default
  private Boolean isActive = true;
}
