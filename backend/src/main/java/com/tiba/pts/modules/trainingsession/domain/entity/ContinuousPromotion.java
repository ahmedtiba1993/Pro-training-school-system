package com.tiba.pts.modules.trainingsession.domain.entity;

import com.tiba.pts.modules.trainingsession.domain.enums.DurationUnit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContinuousPromotion extends Promotion {

  @Column(nullable = false)
  private Integer durationValue;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DurationUnit durationUnit;
}
