package com.tiba.pts.modules.profiles.dto.response;

import lombok.Data;
import java.time.Year;

@Data
public class GraduationRecordResponse {
  private String degreeName;
  private Year graduationYear;
  private String mention;
}
