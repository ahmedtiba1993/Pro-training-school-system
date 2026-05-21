package com.tiba.pts.modules.subject.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.modules.specialty.domain.entity.Specialty;
import com.tiba.pts.modules.specialty.domain.entity.Training;
import com.tiba.pts.modules.subject.domain.enums.SubjectStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Subject extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, updatable = false, length = 50)
  private String code;

  @Column(nullable = false, length = 255)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(nullable = false)
  private Integer theoryHours;

  @Column(nullable = false)
  private Integer practicalHours;

  @Column(nullable = false)
  private Integer totalHours;

  @Column private Double defaultCoefficient;

  @Column(length = 500)
  private String pdfFilePath;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SubjectStatus status;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "training_id", nullable = false)
  private Training training;

  @PrePersist
  @PreUpdate
  public void calculateTotalHours() {
    int th = this.theoryHours != null ? this.theoryHours : 0;
    int ph = this.practicalHours != null ? this.practicalHours : 0;
    this.totalHours = th + ph;

    if (this.totalHours <= 0) {
      throw new BusinessValidationException("TOTAL_HOURS_MUST_BE_GREATER_THAN_ZERO");
    }
  }
}
