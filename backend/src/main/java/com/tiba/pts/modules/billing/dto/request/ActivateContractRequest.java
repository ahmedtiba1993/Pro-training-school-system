package com.tiba.pts.modules.billing.dto.request;

import com.tiba.pts.modules.billing.domain.enums.PaymentMethod;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
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
public class ActivateContractRequest {

  @NotNull(message = "DISCOUNT_AMOUNT_REQUIRED")
  @DecimalMin(value = "0.0", message = "DISCOUNT_AMOUNT_MUST_BE_NON_NEGATIVE")
  private BigDecimal discountAmount;

  @NotNull(message = "PAYMENT_METHOD_REQUIRED")
  private PaymentMethod paymentMethod;

  @NotNull(message = "PAYMENT_DATE_REQUIRED")
  @PastOrPresent(message = "PAYMENT_DATE_CANNOT_BE_IN_THE_FUTURE")
  private LocalDateTime paymentDate;

  private String reference;

  @AssertTrue(message = "REFERENCE_REQUIRED_FOR_CHECK_OR_TRANSFER")
  public boolean isReferenceProvidedWhenRequired() {
    if (paymentMethod == PaymentMethod.CHECK || paymentMethod == PaymentMethod.BANK_TRANSFER) {
      return reference != null && !reference.trim().isEmpty();
    }
    return true;
  }
}
