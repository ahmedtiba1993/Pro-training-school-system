package com.tiba.pts.modules.classmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassManagementStatsResponse {

  private long activeClassesCount;
  private long assignedStudentsInActiveClassesCount;
}
