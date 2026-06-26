package com.tiba.pts.modules.billing.domain.model;

import com.tiba.pts.core.domain.BaseEntity;
import com.tiba.pts.modules.billing.domain.enums.ContractStatus;
import com.tiba.pts.modules.enrollment.domain.entity.Enrollment;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "financial_contracts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialContract extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "financial_contract_seq")
  @SequenceGenerator(name = "financial_contract_seq", sequenceName = "financial_contract_seq")
  private Long id;

  @Column(name = "contract_number", unique = true, nullable = false, updatable = false)
  private String contractNumber;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "enrollment_id", nullable = false, unique = true)
  private Enrollment enrollment;

  @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal totalAmount;

  @Builder.Default
  @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal discountAmount = BigDecimal.ZERO;

  @Builder.Default
  @Column(name = "paid_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal paidAmount = BigDecimal.ZERO;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "varchar(30) default 'DRAFT'")
  private ContractStatus status;
}
