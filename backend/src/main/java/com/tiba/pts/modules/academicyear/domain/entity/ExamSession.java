package com.tiba.pts.modules.academicyear.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import com.tiba.pts.modules.academicyear.domain.enums.SessionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
    name = "exam_sessions",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_term_session_type",
          columnNames = {"term_id", "session_type"})
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamSession extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "exam_session_seq_gen")
  @SequenceGenerator(
      name = "exam_session_seq_gen",
      sequenceName = "exam_session_seq",
      allocationSize = 1)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "session_type", nullable = false)
  private SessionType sessionType;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "term_id", nullable = false)
  private Term term;
}
