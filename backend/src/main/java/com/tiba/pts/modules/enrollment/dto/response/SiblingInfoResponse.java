package com.tiba.pts.modules.enrollment.dto.response;

import lombok.Data;

@Data
public class SiblingInfoResponse {
  private Long id;
  private String fullName;
  private Integer age;
  private String schoolOrWorkplace;
}
