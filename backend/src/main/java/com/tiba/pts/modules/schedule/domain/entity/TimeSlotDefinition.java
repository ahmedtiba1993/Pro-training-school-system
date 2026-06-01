package com.tiba.pts.modules.schedule.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSlotDefinition extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "time_slot_definition_seq")
  @SequenceGenerator(
      name = "time_slot_definition_seq",
      sequenceName = "time_slot_definition_seq",
      allocationSize = 1)
  private Long id;

  @Column(nullable = false, unique = true)
  private String code; // Ex: "S1", "S2"

  @Column(nullable = false)
  private String label; // Ex: "Séance 1"

  @Column(nullable = false)
  private LocalTime startTime; // Ex: 08:00

  @Column(nullable = false)
  private LocalTime endTime; // Ex: 10:00

  @Column(nullable = false)
  private Integer orderIndex;
}
