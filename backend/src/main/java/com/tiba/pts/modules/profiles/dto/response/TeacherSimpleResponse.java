package com.tiba.pts.modules.profiles.dto.response;

import lombok.Data;

@Data
public class TeacherSimpleResponse {
  private Long id;
  private String firstName;
  private String lastName;
  private String phone;
  private String code;
}
