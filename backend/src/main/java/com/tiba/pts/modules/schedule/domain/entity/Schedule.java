package com.tiba.pts.modules.schedule.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import com.tiba.pts.modules.academicyear.domain.entity.Period;
import com.tiba.pts.modules.classmanagement.domain.entity.ClassGroup;
import com.tiba.pts.modules.schedule.domain.enums.ScheduleStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Schedule extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "schedule_seq")
  @SequenceGenerator(name = "schedule_seq", sequenceName = "schedule_seq", allocationSize = 1)
  private Long id;

  @Column(nullable = false)
  private String label;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private ScheduleStatus status = ScheduleStatus.DRAFT;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "class_group_id", nullable = false)
  private ClassGroup classGroup;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "academic_period_id", nullable = true)
  private Period period;
}
