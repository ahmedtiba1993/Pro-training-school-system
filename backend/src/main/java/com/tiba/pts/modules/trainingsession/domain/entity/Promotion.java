package com.tiba.pts.modules.trainingsession.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import com.tiba.pts.modules.specialty.domain.entity.Training;
import com.tiba.pts.modules.trainingsession.domain.enums.PromotionStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import org.hibernate.annotations.Formula;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
public abstract class Promotion extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "promo_seq")
  @SequenceGenerator(name = "promo_seq", sequenceName = "promotion_sequence", allocationSize = 1)
  private Long id;

  private String name;

  @Column(unique = true, nullable = false, updatable = false)
  private String code;

  private LocalDate startDate;

  private LocalDate endDate;

  private LocalDate registrationOpeningDate;

  private LocalDate registrationDeadline;

  @Column(nullable = false)
  private Integer enrollmentCount = 0;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PromotionStatus status = PromotionStatus.DRAFT;

  @Column(precision = 10, scale = 2, nullable = false)
  private BigDecimal registrationFee;

  @Column(precision = 10, scale = 2, nullable = false)
  private BigDecimal tuitionFee;

  @Column(nullable = false)
  private Integer capacity;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "training_id", nullable = false)
  private Training training;
}
