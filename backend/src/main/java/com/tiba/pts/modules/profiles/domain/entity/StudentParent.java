package com.tiba.pts.modules.profiles.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import com.tiba.pts.modules.profiles.domain.enums.ParentalLink;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentParent extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "student_parent_seq")
  @SequenceGenerator(name = "student_parent_seq", sequenceName = "student_parent_seq")
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "parental_link", nullable = false)
  private ParentalLink link;

  @Column(name = "is_legal_guardian", nullable = false)
  private boolean isLegalGuardian;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_id", nullable = false)
  private Student student;

  @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
  @JoinColumn(name = "parent_id", nullable = false)
  private Parent parent;
}
