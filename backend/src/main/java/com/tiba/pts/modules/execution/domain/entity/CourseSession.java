package com.tiba.pts.modules.execution.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import com.tiba.pts.modules.execution.domain.enums.SessionStatus;
import com.tiba.pts.modules.execution.domain.enums.SessionType;
import com.tiba.pts.modules.classmanagement.domain.entity.ClassGroup;
import com.tiba.pts.modules.subject.domain.entity.Subject;
import com.tiba.pts.modules.profiles.domain.entity.Teacher;
import com.tiba.pts.modules.room.domain.entity.Room;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseSession extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "session_seq")
  @SequenceGenerator(name = "session_seq", sequenceName = "session_seq", allocationSize = 1)
  private Long id;

  @Column(nullable = false)
  private LocalDate sessionDate;

  @Column(nullable = false)
  private LocalTime startTime;

  @Column(nullable = false)
  private LocalTime endTime;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SessionType sessionType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private SessionStatus status = SessionStatus.PLANNED;

  private String lessonTitle;

  @Column(columnDefinition = "TEXT")
  private String courseContent;

  @Builder.Default
  @Column(nullable = false)
  private boolean isAttendanceSubmitted = false;

  private String cancellationReason;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "class_group_id", nullable = false)
  private ClassGroup classGroup;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "subject_id", nullable = false)
  private Subject subject;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "teacher_id", nullable = false)
  private Teacher teacher;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "room_id")
  private Room room;

  @OneToMany(mappedBy = "courseSession", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<AttendanceRecord> attendances = new ArrayList<>();

  public void clearAttendances() {
    if (this.attendances != null) {
      this.attendances.clear();
    }
  }
}

