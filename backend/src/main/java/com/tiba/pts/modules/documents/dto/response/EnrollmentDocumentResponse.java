package com.tiba.pts.modules.documents.dto.response;

import com.tiba.pts.modules.documents.domain.enums.DocumentCondition;
import com.tiba.pts.modules.documents.domain.enums.DocumentNature;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentDocumentResponse {
  private Long id;
  private String code;
  private String label;
  private String labelAr;
  private Integer quantity;
  private DocumentNature nature;
  private DocumentCondition condition;
  private List<LevelSummaryResponse> levels;
  private Boolean mandatory;
  private Boolean isActive;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LevelSummaryResponse {
    private Long id;
    private String code;
    private String label;
  }
}
