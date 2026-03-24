package com.tiba.pts.modules.person.domain.entity;

import com.tiba.pts.modules.person.domain.enums.MaritalStatus;
import com.tiba.pts.modules.person.domain.enums.ParentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "parents")
@PrimaryKeyJoinColumn(name = "person_id")
public class Parent extends Person {

  private String profession;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ParentType parentType;

  @Enumerated(EnumType.STRING)
  private MaritalStatus maritalStatus;

  @Column(nullable = false, columnDefinition = "boolean default false")
  private boolean isLegalGuardian = false;

  @Column(nullable = false)
  private boolean isDeceased = false;

  @ManyToMany
  @JoinTable(
      name = "parent_student",
      joinColumns = @JoinColumn(name = "parent_id"),
      inverseJoinColumns = @JoinColumn(name = "student_id"))
  private Set<Student> children = new HashSet<>();
}
