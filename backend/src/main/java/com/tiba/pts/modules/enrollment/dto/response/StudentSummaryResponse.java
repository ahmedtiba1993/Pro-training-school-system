package com.tiba.pts.modules.enrollment.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class StudentSummaryResponse {
  private Long id;
  private String studentCode;
  private String firstName;
  private String lastName;
  private String cin;
  private String phone;
  private List<StudentParentSummaryResponse> parents;
}
