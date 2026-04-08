package com.tiba.pts.modules.specialty.domain.enums;

import lombok.Getter;

@Getter
public enum AccessLevel {
  GRADE_9("9b", "9th Grade completed or equivalent"),
  SECONDARY_1("1s", "1st Year of Secondary School completed"),
  SECONDARY_2("2s", "2nd Year of Secondary School completed or equivalent"),
  SECONDARY_3("3s", "3rd Year of Secondary School completed"),
  BACCALAUREATE("bac", "Baccalaureate completed or equivalent"),
  BACHELORS_DEGREE("licence", "Bachelor's Degree or equivalent"),
  NONE("none", "No level requirement (Open access)");

  private final String code;
  private final String label;

  AccessLevel(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
