package com.tiba.pts.modules.grading.dto.response;

import com.tiba.pts.modules.grading.domain.enums.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeRecordResponse {
  private Long id;
  private Long enrollmentId;
  private String matricule;
  private String nom;
  private String prenom;
  private AttendanceStatus presence;
  private BigDecimal note;
  private String commentaire;
}
