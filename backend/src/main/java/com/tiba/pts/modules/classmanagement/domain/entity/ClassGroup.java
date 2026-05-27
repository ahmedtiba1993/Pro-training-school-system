package com.tiba.pts.modules.classmanagement.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import com.tiba.pts.modules.classmanagement.domain.enums.ClassStatus;
import com.tiba.pts.modules.trainingsession.domain.entity.Promotion;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassGroup extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "class_group_seq")
  @SequenceGenerator(name = "class_group_seq", sequenceName = "class_group_seq", allocationSize = 1)
  private Long id;

  @Column(nullable = false, unique = true)
  private String code;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private Integer capacity;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private ClassStatus status = ClassStatus.DRAFT;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "promotion_id", nullable = false)
  private Promotion promotion;

  @OneToMany(mappedBy = "classGroup", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<ClassAssignment> assignments = new ArrayList<>();

  /** Dynamically computed field (Zero database synchronization debt). */
  @Transient
  public Integer getCurrentSize() {
    return this.assignments != null ? this.assignments.size() : 0;
  }
}
