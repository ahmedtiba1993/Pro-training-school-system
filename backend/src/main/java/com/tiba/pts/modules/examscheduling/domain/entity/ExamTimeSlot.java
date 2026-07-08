package com.tiba.pts.modules.examscheduling.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "exam_time_slots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamTimeSlot extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "exam_time_slot_seq")
  @SequenceGenerator(
      name = "exam_time_slot_seq",
      sequenceName = "exam_time_slot_seq",
      allocationSize = 1)
  private Long id;

  @Column(nullable = false, unique = true)
  private String code;

  @Column(nullable = false)
  private String label;

  @Column(nullable = false)
  private LocalTime startTime;

  @Column(nullable = false)
  private LocalTime endTime;

  @Column(nullable = false)
  @Builder.Default
  private Boolean isActive = true;
}
