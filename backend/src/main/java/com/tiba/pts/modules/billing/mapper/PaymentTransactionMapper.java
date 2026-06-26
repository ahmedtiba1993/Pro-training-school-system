package com.tiba.pts.modules.billing.mapper;

import com.tiba.pts.modules.billing.domain.model.FinancialContract;
import com.tiba.pts.modules.billing.domain.model.PaymentTransaction;
import com.tiba.pts.modules.billing.dto.request.PaymentTransactionRequest;
import com.tiba.pts.modules.billing.dto.response.PaymentTransactionResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface PaymentTransactionMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "status", constant = "PENDING") // Default initial status
  @Mapping(target = "financialContract", ignore = true)
  @Mapping(target = "receiptNumber", ignore = true)
  PaymentTransaction toEntity(PaymentTransactionRequest request);

  @Mapping(target = "financialContractId", source = "financialContract.id")
  @Mapping(target = "contractNumber", source = "financialContract.contractNumber")
  PaymentTransactionResponse toResponse(PaymentTransaction transaction);
}

