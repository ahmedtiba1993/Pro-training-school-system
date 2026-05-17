package com.tiba.pts.modules.profiles.dto.response;

import lombok.Data;

@Data
public class RefTeacherSpecialtyResponse {
  private Long id;
  private String code;
  private String label;
  private String description;

  private int teacherCount;
}
