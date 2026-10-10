package com.tiba.pts.modules.specialty.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import com.tiba.pts.modules.specialty.domain.enums.AccessLevel;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Level extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "level_seq")
  @SequenceGenerator(name = "level_seq", sequenceName = "level_seq", allocationSize = 1)
  private Long id;

  @Column(nullable = false, unique = true)
  private String code;

  @Column(nullable = false)
  private String label;

  @Column(nullable = false)
  @Builder.Default
  private Boolean isActive = true;

  @Enumerated(EnumType.STRING)
  private AccessLevel accessLevel;

  @OneToMany(mappedBy = "level")
  @Builder.Default
  private List<Training> trainings = new ArrayList<>();
}
