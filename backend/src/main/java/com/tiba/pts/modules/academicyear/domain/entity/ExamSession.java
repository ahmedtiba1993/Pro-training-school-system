package com.tiba.pts.modules.academicyear.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import com.tiba.pts.modules.academicyear.domain.enums.SessionStatus;
import com.tiba.pts.modules.academicyear.domain.enums.SessionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "exam_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamSession {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "exam_session_seq")
  @SequenceGenerator(
      name = "exam_session_seq",
      sequenceName = "exam_session_seq",
      allocationSize = 1)
  private Long id;

  @Column(nullable = false)
  private String label;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SessionType sessionType;

  @Column(nullable = false)
  private LocalDate startDate;

  @Column(nullable = false)
  private LocalDate endDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SessionStatus status;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "period_id", nullable = false)
  private Period period;
}
