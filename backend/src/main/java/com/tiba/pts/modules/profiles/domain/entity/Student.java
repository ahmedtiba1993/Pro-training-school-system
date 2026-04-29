package com.tiba.pts.modules.profiles.domain.entity;

import com.tiba.pts.modules.profiles.domain.enums.ParentsSituation;
import com.tiba.pts.modules.profiles.domain.enums.StudentResidence;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@PrimaryKeyJoinColumn(name = "person_id")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Student extends Person {

  @Column(unique = true, nullable = false)
  private String studentCode;

  @Column(name = "birth_date", nullable = false)
  private LocalDate birthDate;

  private String birthPlace;

  private String governorate;

  private String delegation;

  private String residenceAddress;

  private String correspondenceAddress;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private StudentResidence residence;

  @Enumerated(EnumType.STRING)
  private ParentsSituation parentsSituation;

  private boolean hasChronicDisease;

  @Column(name = "disease_description", length = 500)
  private String diseaseDescription;

  @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<StudentSibling> studentSiblings = new ArrayList<>();

  @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<StudentParent> parents = new ArrayList<>();
}
