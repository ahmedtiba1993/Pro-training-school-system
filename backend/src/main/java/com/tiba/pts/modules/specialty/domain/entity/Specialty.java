package com.tiba.pts.modules.specialty.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Specialty extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "specialty_seq")
  @SequenceGenerator(name = "specialty_seq", sequenceName = "specialty_seq")
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, unique = true)
  private String code;

  // Relation Plusieurs-à-Plusieurs avec l'entité Level
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "specialty_levels",
      joinColumns = @JoinColumn(name = "specialty_id"),
      inverseJoinColumns = @JoinColumn(name = "level_id"))
  @Builder.Default
  private Set<Level> associatedLevels = new HashSet<>();
}
