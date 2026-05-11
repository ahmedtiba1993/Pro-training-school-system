package com.tiba.pts.modules.trainingsession.domain.entity;

import com.tiba.pts.modules.trainingsession.domain.enums.DurationUnit;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AcceleratedPromotion extends Promotion {

  @Column(nullable = false)
  private Integer durationValue;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DurationUnit durationUnit;
}
