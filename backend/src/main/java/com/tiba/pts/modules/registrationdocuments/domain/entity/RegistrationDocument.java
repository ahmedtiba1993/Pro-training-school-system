package com.tiba.pts.modules.registrationdocuments.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import com.tiba.pts.modules.registrationdocuments.domain.enums.DocumentCondition;
import com.tiba.pts.modules.registrationdocuments.domain.enums.DocumentNature;
import com.tiba.pts.modules.specialty.domain.entity.Level;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationDocument extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "registration_document_seq")
  @SequenceGenerator(name = "registration_document_seq", sequenceName = "registration_document_seq")
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private Integer quantity;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DocumentNature nature;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DocumentCondition condition;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "registration_document_levels",
      joinColumns = @JoinColumn(name = "document_id"),
      inverseJoinColumns = @JoinColumn(name = "level_id"))
  private Set<Level> levels = new HashSet<>();

  @Column(name = "mandatory", nullable = false)
  private Boolean mandatory;
}
