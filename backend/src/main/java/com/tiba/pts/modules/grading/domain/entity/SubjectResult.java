package com.tiba.pts.modules.grading.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import com.tiba.pts.modules.grading.domain.enums.SubjectResultStatus;
import com.tiba.pts.modules.trainingsession.domain.entity.PromotionSubject;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
    name = "subject_results",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"transcript_id", "promotion_subject_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectResult extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "subject_result_seq")
  @SequenceGenerator(name = "subject_result_seq", sequenceName = "subject_result_seq", allocationSize = 1)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "transcript_id", nullable = false)
  private Transcript transcript;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "promotion_subject_id", nullable = false)
  private PromotionSubject promotionSubject;

  @Column(name = "subject_average", precision = 5, scale = 2)
  private BigDecimal subjectAverage;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private SubjectResultStatus status;
}
