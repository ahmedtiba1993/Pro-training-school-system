package com.tiba.pts.modules.grading.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import com.tiba.pts.modules.grading.domain.enums.AssessmentStatus;
import com.tiba.pts.modules.grading.domain.enums.AssessmentType;
import com.tiba.pts.modules.trainingsession.domain.entity.PromotionSubject;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "assessments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assessment extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "assessment_seq")
  @SequenceGenerator(name = "assessment_seq", sequenceName = "assessment_seq", allocationSize = 1)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "promotion_subject_id", nullable = false)
  private PromotionSubject promotionSubject;

  @Column(nullable = false)
  private String title;

  @Enumerated(EnumType.STRING)
  @Column(name = "assessment_type", nullable = false, length = 30)
  private AssessmentType assessmentType;

  @Column(name = "total_marks", nullable = false)
  private Double totalMarks;

  @Column(name = "weight_percentage", nullable = false)
  private Integer weightPercentage;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private AssessmentStatus status;
}
