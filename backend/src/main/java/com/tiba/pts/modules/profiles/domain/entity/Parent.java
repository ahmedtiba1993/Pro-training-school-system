package com.tiba.pts.modules.profiles.domain.entity;

import com.tiba.pts.modules.profiles.domain.enums.ParentalLink;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@PrimaryKeyJoinColumn(name = "person_id")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Parent extends Person {

  @Column(nullable = false)
  private String profession;

  @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<StudentParent> studentLinks = new ArrayList<>();
}
