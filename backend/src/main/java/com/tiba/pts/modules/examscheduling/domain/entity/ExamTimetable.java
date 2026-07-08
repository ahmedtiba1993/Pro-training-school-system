package com.tiba.pts.modules.examscheduling.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import com.tiba.pts.modules.classmanagement.domain.entity.ClassGroup;
import com.tiba.pts.modules.academicyear.domain.entity.Period;
import com.tiba.pts.modules.academicyear.domain.entity.ExamSession;
import com.tiba.pts.modules.examscheduling.domain.enums.ExamTimetableStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exam_timetables")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamTimetable extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "exam_timetable_seq")
  @SequenceGenerator(
      name = "exam_timetable_seq",
      sequenceName = "exam_timetable_seq",
      allocationSize = 1)
  private Long id;

  @Column(nullable = false)
  private String name;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "class_group_id", nullable = false)
  private ClassGroup classGroup;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "period_id", nullable = false)
  private Period period;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "exam_session_id", nullable = false)
  private ExamSession examSession;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private ExamTimetableStatus status;

  @OneToMany(mappedBy = "examTimetable", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<ExamSchedule> schedules = new ArrayList<>();
}
