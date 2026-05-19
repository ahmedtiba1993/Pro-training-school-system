package com.tiba.pts.modules.subject.dto.response;

import com.tiba.pts.modules.subject.domain.enums.SubjectStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SubjectResponse {
  private Long id;
  private String code;
  private String name;
  private String description;
  private Integer theoryHours;
  private Integer practicalHours;
  private Integer totalHours;
  private Double defaultCoefficient;
  private String pdfFilePath;
  private SubjectStatus status;
  private Long specialtyId;
  private String specialtyLabel;
}
