package com.tiba.pts.modules.billing.domain.model;

import com.tiba.pts.core.domain.BaseEntity;
import com.tiba.pts.modules.billing.domain.enums.PaymentMethod;
import com.tiba.pts.modules.billing.domain.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "payment_transaction_seq")
  @SequenceGenerator(name = "payment_transaction_seq", sequenceName = "payment_transaction_seq")
  private Long id;

  @Column(name = "receipt_number", unique = true, nullable = false, updatable = false)
  private String receiptNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "financial_contract_id", nullable = false)
  private FinancialContract financialContract;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_method", nullable = false)
  private PaymentMethod paymentMethod;

  @Column(name = "payment_date", nullable = false)
  private LocalDateTime paymentDate;

  @Column(length = 500)
  private String reference;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "varchar(30) default 'PENDING'")
  private TransactionStatus status;
}
