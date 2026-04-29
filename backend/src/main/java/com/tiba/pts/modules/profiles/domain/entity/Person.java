package com.tiba.pts.modules.profiles.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "persons")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public abstract class Person extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "person_seq")
  @SequenceGenerator(name = "person_seq", sequenceName = "person_seq", allocationSize = 1)
  private Long id;

  @Column(name = "first_name", nullable = false)
  private String firstName;

  @Column(name = "last_name", nullable = false)
  private String lastName;

  @Column(name = "first_name_ar")
  private String firstNameAr;

  @Column(name = "last_name_ar")
  private String lastNameAr;

  @Column(unique = true)
  private String email;

  private String phone;

  @Column(unique = true)
  private String cin;
}
