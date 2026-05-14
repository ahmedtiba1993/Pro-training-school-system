package com.tiba.pts.modules.specialty.domain.enums;

import lombok.Getter;

@Getter
public enum TrainingType {
  CONTINUOUS("Continue"),
  ACCELERATED("Accélérée"),
  ACCREDITED("Homologuée");

  private final String labelFr;

  TrainingType(String labelFr) {
    this.labelFr = labelFr;
  }

  public String getLabelFr() {
    return labelFr;
  }
}
