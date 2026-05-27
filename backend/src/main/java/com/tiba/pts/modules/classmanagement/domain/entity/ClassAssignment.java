package com.tiba.pts.modules.classmanagement.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import com.tiba.pts.modules.enrollment.domain.entity.Enrollment;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassAssignment extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "class_assignment_seq")
  @SequenceGenerator(
      name = "class_assignment_seq",
      sequenceName = "class_assignment_seq",
      allocationSize = 1)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "class_group_id", nullable = false)
  private ClassGroup classGroup;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "enrollment_id", nullable = false)
  private Enrollment enrollment;
}
