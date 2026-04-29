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
  @SequenceGenerator(name = "level_seq", sequenceName = "level_seq")
  private Long id;

  @Column(nullable = false, unique = true)
  private String code;

  @Column(nullable = false)
  private String label;

  @Enumerated(EnumType.STRING)
  private AccessLevel accessLevel;

  @OneToMany(mappedBy = "level", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Training> trainings = new ArrayList<>();
}
