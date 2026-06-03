package com.tiba.pts.modules.execution.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import com.tiba.pts.modules.enrollment.domain.entity.Enrollment;
import com.tiba.pts.modules.execution.domain.enums.AttendanceStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRecord extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "attendance_record_seq")
  @SequenceGenerator(name = "attendance_record_seq", sequenceName = "attendance_record_seq", allocationSize = 1)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "course_session_id", nullable = false)
  private CourseSession courseSession;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "enrollment_id", nullable = false)
  private Enrollment enrollment;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AttendanceStatus status;

  private String details;

  private String adminNotes;
}
