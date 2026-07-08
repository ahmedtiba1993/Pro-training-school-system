package com.tiba.pts.modules.grading.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import com.tiba.pts.modules.enrollment.domain.entity.Enrollment;
import com.tiba.pts.modules.grading.domain.enums.AttendanceStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(
    name = "grade_records",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"assessment_id", "enrollment_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradeRecord extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "grade_record_seq")
  @SequenceGenerator(name = "grade_record_seq", sequenceName = "grade_record_seq", allocationSize = 1)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "assessment_id", nullable = false)
  private Assessment assessment;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "enrollment_id", nullable = false)
  private Enrollment enrollment;

  @Column(name = "score", precision = 5, scale = 2)
  private BigDecimal score;

  @Column(name = "teacher_comment", columnDefinition = "TEXT")
  private String teacherComment;

  @Enumerated(EnumType.STRING)
  @Column(name = "attendance_status", nullable = false, length = 30)
  private AttendanceStatus attendanceStatus;
}
