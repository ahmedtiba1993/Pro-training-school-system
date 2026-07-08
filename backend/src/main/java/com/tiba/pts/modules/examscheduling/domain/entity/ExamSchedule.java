package com.tiba.pts.modules.examscheduling.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import com.tiba.pts.modules.grading.domain.entity.Assessment;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exam_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamSchedule extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "exam_schedule_seq")
  @SequenceGenerator(
      name = "exam_schedule_seq",
      sequenceName = "exam_schedule_seq",
      allocationSize = 1)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "exam_timetable_id", nullable = false)
  private ExamTimetable examTimetable;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "assessment_id", nullable = false)
  private Assessment assessment;

  @Column(name = "exam_date", nullable = false)
  private LocalDate examDate;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "exam_time_slot_id", nullable = false)
  private ExamTimeSlot examTimeSlot;

  @OneToMany(mappedBy = "examSchedule", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<ExamRoomAllocation> roomAllocations = new ArrayList<>();
}
