package com.tiba.pts.modules.trainingsession.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import com.tiba.pts.modules.academicyear.domain.entity.Period;
import com.tiba.pts.modules.subject.domain.entity.Subject;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionSubject extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "promotion_subject_seq")
  @SequenceGenerator(
      name = "promotion_subject_seq",
      sequenceName = "promotion_subject_seq",
      allocationSize = 1)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "promotion_id", nullable = false)
  private Promotion promotion;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "subject_id", nullable = false)
  private Subject subject;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "academic_period_id", nullable = true)
  private Period academicPeriod;

  @Column(nullable = false)
  private Double coefficient;
}
