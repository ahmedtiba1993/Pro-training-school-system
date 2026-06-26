package com.tiba.pts.modules.billing.dto.response;

import com.tiba.pts.modules.billing.domain.enums.PaymentMethod;
import com.tiba.pts.modules.billing.domain.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransactionResponse {

  private Long id;
  private String receiptNumber;
  private Long financialContractId;
  private String contractNumber;
  private BigDecimal amount;
  private PaymentMethod paymentMethod;
  private LocalDateTime paymentDate;
  private String reference;
  private TransactionStatus status;
  private LocalDateTime createdDate;
  private LocalDateTime modifiedDate;
}
