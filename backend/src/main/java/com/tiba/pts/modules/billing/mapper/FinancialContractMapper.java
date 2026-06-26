package com.tiba.pts.modules.billing.mapper;

import com.tiba.pts.modules.billing.domain.model.FinancialContract;
import com.tiba.pts.modules.billing.dto.response.FinancialContractListResponse;
import com.tiba.pts.modules.enrollment.domain.entity.Enrollment;
import com.tiba.pts.modules.profiles.domain.entity.StudentParent;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface FinancialContractMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "contractNumber", source = "contractNumber")
  @Mapping(target = "enrollment", source = "enrollment")
  @Mapping(target = "totalAmount", source = "totalAmount")
  @Mapping(target = "discountAmount", constant = "0")
  @Mapping(target = "paidAmount", constant = "0")
  @Mapping(target = "status", constant = "DRAFT")
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "modifiedDate", ignore = true)
  FinancialContract toEntity(String contractNumber, Enrollment enrollment, BigDecimal totalAmount);

  @Mapping(target = "promotionName", source = "enrollment.promotion.name")
  @Mapping(target = "studentFirstName", source = "enrollment.student.firstName")
  @Mapping(target = "studentLastName", source = "enrollment.student.lastName")
  @Mapping(target = "studentPhone", source = "enrollment.student.phone") 
  @Mapping(target = "totalCollected", source = "paidAmount")
  @Mapping(target = "netToPay", source = "contract", qualifiedByName = "calculateNetToPay")
  @Mapping(target = "remainingDebt", source = "contract", qualifiedByName = "calculateRemainingDebt")
  @Mapping(target = "guardianPhone", source = "contract", qualifiedByName = "extractGuardianPhone")
  FinancialContractListResponse toListResponse(FinancialContract contract);

  List<FinancialContractListResponse> toListResponseCollection(List<FinancialContract> contracts);

  @Named("calculateNetToPay")
  default BigDecimal calculateNetToPay(FinancialContract contract) {
    if (contract == null || contract.getTotalAmount() == null) return BigDecimal.ZERO;
    BigDecimal discount = contract.getDiscountAmount() != null ? contract.getDiscountAmount() : BigDecimal.ZERO;
    return contract.getTotalAmount().subtract(discount);
  }

  @Named("calculateRemainingDebt")
  default BigDecimal calculateRemainingDebt(FinancialContract contract) {
    if (contract == null || contract.getTotalAmount() == null) return BigDecimal.ZERO;
    BigDecimal discount = contract.getDiscountAmount() != null ? contract.getDiscountAmount() : BigDecimal.ZERO;
    BigDecimal paid = contract.getPaidAmount() != null ? contract.getPaidAmount() : BigDecimal.ZERO;
    return contract.getTotalAmount().subtract(discount).subtract(paid);
  }

  @Named("extractGuardianPhone")
  default String extractGuardianPhone(FinancialContract contract) {
    if (contract == null || contract.getEnrollment() == null || contract.getEnrollment().getStudent() == null) {
      return null;
    }
    List<StudentParent> studentParents = contract.getEnrollment().getStudent().getParents();
    if (studentParents == null) return null;

    return studentParents.stream()
        .filter(StudentParent::isLegalGuardian)
        .map(sp -> sp.getParent() != null ? sp.getParent().getPhone() : null)
        .filter(phone -> phone != null && !phone.isEmpty())
        .findFirst()
        .orElse(null);
  }
}

