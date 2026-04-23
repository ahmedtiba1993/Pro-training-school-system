package com.tiba.pts.modules.documents.dto.response;

import com.tiba.pts.modules.documents.domain.enums.DocumentCondition;
import com.tiba.pts.modules.documents.domain.enums.DocumentNature;
import com.tiba.pts.modules.specialty.dto.request.LevelRequest;
import com.tiba.pts.modules.specialty.dto.response.LevelResponse;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class EnrollmentDocumentResponse {
  private Long id;
  private String name;
  private Integer quantity;
  private DocumentNature nature;
  private DocumentCondition condition;
  private List<LevelSummaryResponse> levels;
  private Boolean mandatory;

  @Data
  public static class LevelSummaryResponse {
    private Long id;
    private String code;
    private String label;
  }
}
