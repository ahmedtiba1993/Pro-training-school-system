package com.tiba.pts.modules.specialty.domain.enums;

import lombok.Getter;

@Getter
public enum TrainingType {
  CONTINUOUS("Continue", "CON"),
  ACCELERATED("Accélérée", "ACC"),
  ACCREDITED("Homologuée", "HOM");

  private final String labelFr;
  private final String prefix;

  TrainingType(String labelFr, String prefix) {
    this.labelFr = labelFr;
    this.prefix = prefix;
  }
}
