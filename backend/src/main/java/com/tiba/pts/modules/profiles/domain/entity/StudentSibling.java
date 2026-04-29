package com.tiba.pts.modules.profiles.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "siblings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentSibling extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "StudentSibling_seq")
  @SequenceGenerator(name = "StudentSibling_seq", sequenceName = "StudentSibling_seq")
  private Long id;

  @Column(name = "full_name", nullable = false)
  private String fullName;

  @Column(nullable = false)
  private Integer age;

  @Column(name = "school_or_workplace")
  private String schoolOrWorkplace;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_id", nullable = false)
  private Student student;
}
