package com.tiba.pts.modules.schedule.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import com.tiba.pts.modules.profiles.domain.entity.Teacher;
import com.tiba.pts.modules.schedule.domain.enums.DayOfWeek;
import com.tiba.pts.modules.schedule.domain.enums.Periodicity;
import com.tiba.pts.modules.subject.domain.entity.Subject;
import com.tiba.pts.modules.room.domain.entity.Room;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimetableSlot extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "timetable_slot_seq")
  @SequenceGenerator(
      name = "timetable_slot_seq",
      sequenceName = "timetable_slot_seq",
      allocationSize = 1)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DayOfWeek dayOfWeek;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Periodicity periodicity;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "schedule_id", nullable = false)
  private Schedule schedule;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "subject_id", nullable = false)
  private Subject subject;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "teacher_id", nullable = false)
  private Teacher teacher;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "room_id", nullable = false)
  private Room room;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "time_slot_definition_id", nullable = false)
  private TimeSlotDefinition timeSlotDefinition;
}
