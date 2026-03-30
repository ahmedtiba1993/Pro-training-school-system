package com.tiba.pts.modules.person.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "students")
@PrimaryKeyJoinColumn(name = "person_id")
public class Student extends Person {

  private LocalDate dateOfBirth;

  @Column(name = "education_level")
  private String educationLevel;

  @ManyToMany(mappedBy = "children")
  private Set<Parent> parents = new HashSet<>();
}
