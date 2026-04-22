package com.tiba.pts.modules.trainingsession.domain.entity;

import jakarta.persistence.Entity;
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

  private Integer numberOfHours;
  private String level;
}
