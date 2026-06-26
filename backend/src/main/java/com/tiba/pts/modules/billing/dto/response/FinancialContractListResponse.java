package com.tiba.pts.modules.billing.dto.response;

import com.tiba.pts.modules.billing.domain.enums.ContractStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialContractListResponse {

  private Long id;
  private String contractNumber;
  private String promotionName;
  private String studentFirstName;
  private String studentLastName;
  private String studentPhone;
  private String guardianPhone;
  private BigDecimal totalAmount;
  private BigDecimal discountAmount;
  private BigDecimal netToPay; 
  private BigDecimal totalCollected; 
  private BigDecimal remainingDebt; 
  private ContractStatus status;
}
