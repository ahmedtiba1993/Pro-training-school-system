package com.tiba.pts.modules.billing.dto.request;

import com.tiba.pts.modules.billing.domain.enums.ContractStatus;
import com.tiba.pts.modules.specialty.domain.enums.TrainingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialContractSearchRequest {

  private String keyword;
  private ContractStatus status;
  private Long promotionId;
  private TrainingType trainingType;

  @Builder.Default private int page = 0;

  @Builder.Default private int size = 10;

  public String getKeyword() {
    return (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
  }
}
