package com.tiba.pts.modules.profiles.dto.response;

import lombok.Data;

@Data
public class StudentSiblingResponse {
  private Long id;
  private String fullName;
  private Integer age;
  private String schoolOrWorkplace;
}
