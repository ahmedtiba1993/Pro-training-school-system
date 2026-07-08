package com.tiba.pts.modules.grading.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import com.tiba.pts.modules.academicyear.domain.entity.ExamSession;
import com.tiba.pts.modules.enrollment.domain.entity.Enrollment;
import com.tiba.pts.modules.grading.domain.enums.JuryDecision;
import com.tiba.pts.modules.grading.domain.enums.TranscriptStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
    name = "transcripts",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"enrollment_id", "exam_session_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transcript extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transcript_seq")
  @SequenceGenerator(name = "transcript_seq", sequenceName = "transcript_seq", allocationSize = 1)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "enrollment_id", nullable = false)
  private Enrollment enrollment;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "exam_session_id", nullable = false)
  private ExamSession examSession;

  @Column(name = "overall_average", precision = 5, scale = 2)
  private BigDecimal overallAverage;

  @Enumerated(EnumType.STRING)
  @Column(name = "jury_decision", nullable = false, length = 30)
  private JuryDecision juryDecision;

  @Column(name = "general_appreciation", columnDefinition = "TEXT")
  private String generalAppreciation;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private TranscriptStatus status;
}
