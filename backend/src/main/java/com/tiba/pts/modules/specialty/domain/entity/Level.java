package com.tiba.pts.modules.specialty.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import com.tiba.pts.modules.specialty.domain.enums.TrainingType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Level extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "training_seq")
  @SequenceGenerator(name = "training_seq", sequenceName = "training_seq")
  private Long id;

  @Column(nullable = false, unique = true)
  private String code;

  @Column(nullable = false)
  private String label;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TrainingType type;
}
