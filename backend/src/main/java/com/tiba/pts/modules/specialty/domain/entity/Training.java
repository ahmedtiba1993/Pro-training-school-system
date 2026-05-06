package com.tiba.pts.modules.specialty.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import com.tiba.pts.modules.specialty.domain.enums.DurationUnit;
import com.tiba.pts.modules.specialty.domain.enums.TrainingStatus;
import com.tiba.pts.modules.specialty.domain.enums.TrainingType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Table(
    name = "training",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_training_type_level_specialty",
          columnNames = {"training_type", "level_id", "specialty_id"})
    })
public class Training extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "training_seq")
  @SequenceGenerator(name = "training_seq", sequenceName = "training_seq", allocationSize = 1)
  private Long id;

  @Column(nullable = false, length = 50)
  private String code;

  @Enumerated(EnumType.STRING)
  @Column(name = "training_type", nullable = false)
  private TrainingType trainingType;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private TrainingStatus status = TrainingStatus.DRAFT;

  @Column(name = "duration_value", nullable = false)
  private Integer durationValue;

  @Enumerated(EnumType.STRING)
  @Column(name = "duration_unit", nullable = false)
  private DurationUnit durationUnit;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "level_id", nullable = false)
  private Level level;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "specialty_id", nullable = false)
  private Specialty specialty;
}
